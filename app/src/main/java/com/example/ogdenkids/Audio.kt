package com.example.ogdenkids

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * 全局音频闸门：任意新播放前先停掉其它声道，避免连点答题时多路 MediaPlayer / TTS 叠在一起。
 * 所有 stop/release 都包在 runCatching 里，防止状态异常导致泄漏或崩溃。
 */
internal object OgdenAudioHub {
    private val lock = Any()
    /** 单词/例句播放世代号：过期的 prepareAsync 回调不再 start */
    private val wordGen = AtomicInteger(0)

    @Volatile
    private var wordPlayer: MediaPlayer? = null

    @Volatile
    private var ttsEngine: TextToSpeech? = null

    private val sfxPlayers = mutableMapOf<SfxCue, MediaPlayer>()

    fun attachTts(engine: TextToSpeech?) {
        synchronized(lock) { ttsEngine = engine }
    }

    /** 停止一切正在播的内容（SFX + 单词 + TTS），不销毁 SFX 缓存（可再 seek 重用）。 */
    fun stopAllPlayback() {
        synchronized(lock) {
            wordGen.incrementAndGet()
            stopAndReleaseWordPlayerLocked()
            runCatching { ttsEngine?.stop() }
            sfxPlayers.values.forEach { stopPlayerKeepPrepared(it) }
        }
    }

    fun stopSfxOnly() {
        synchronized(lock) {
            sfxPlayers.values.forEach { stopPlayerKeepPrepared(it) }
        }
    }

    fun playSfx(context: Context, cue: SfxCue) {
        synchronized(lock) {
            // 新音效：先停掉单词/TTS 与其它 SFX，只留这一路
            wordGen.incrementAndGet()
            stopAndReleaseWordPlayerLocked()
            runCatching { ttsEngine?.stop() }
            sfxPlayers.forEach { (key, mp) ->
                if (key != cue) stopPlayerKeepPrepared(mp)
            }
            val mp = sfxPlayers.getOrPut(cue) {
                val fd = context.assets.openFd("audio/sfx/${cue.file}.mp3")
                try {
                    MediaPlayer().apply {
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        prepare()
                    }
                } finally {
                    fd.close()
                }
            }
            stopPlayerKeepPrepared(mp)
            runCatching {
                mp.seekTo(0)
                mp.start()
            }.onFailure {
                // 缓存坏了：丢掉重建
                runCatching { mp.release() }
                sfxPlayers.remove(cue)
                val fd = context.assets.openFd("audio/sfx/${cue.file}.mp3")
                try {
                    val fresh = MediaPlayer().apply {
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        prepare()
                        start()
                    }
                    sfxPlayers[cue] = fresh
                } finally {
                    fd.close()
                }
            }
        }
    }

    /**
     * @return 本次播放世代号，回调里需核对，避免快速连点时旧 prepare 完成仍 start
     */
    fun beginWordPlay(): Int {
        synchronized(lock) {
            val gen = wordGen.incrementAndGet()
            // 新单词：停 SFX + TTS + 旧单词
            sfxPlayers.values.forEach { stopPlayerKeepPrepared(it) }
            runCatching { ttsEngine?.stop() }
            stopAndReleaseWordPlayerLocked()
            return gen
        }
    }

    fun isWordPlayCurrent(gen: Int): Boolean = wordGen.get() == gen

    fun setWordPlayer(player: MediaPlayer?, gen: Int) {
        synchronized(lock) {
            if (gen != wordGen.get()) {
                // 已过期：立刻销毁，勿挂到 hub
                runCatching { player?.stop() }
                runCatching { player?.release() }
                return
            }
            // 若还有更早的引用，先释放
            if (wordPlayer !== null && wordPlayer !== player) {
                stopAndReleaseWordPlayerLocked()
            }
            wordPlayer = player
        }
    }

    fun clearWordPlayerIfSame(player: MediaPlayer?) {
        synchronized(lock) {
            if (wordPlayer === player) wordPlayer = null
        }
    }

    fun releaseAllSfx() {
        synchronized(lock) {
            sfxPlayers.values.forEach { p ->
                runCatching { p.stop() }
                runCatching { p.release() }
            }
            sfxPlayers.clear()
        }
    }

    fun releaseWordPlayer() {
        synchronized(lock) {
            wordGen.incrementAndGet()
            stopAndReleaseWordPlayerLocked()
        }
    }

    private fun stopAndReleaseWordPlayerLocked() {
        val p = wordPlayer
        wordPlayer = null
        if (p != null) {
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.reset() }
            runCatching { p.release() }
        }
    }

    /** 停播但保留 prepare，便于 SFX 复用 */
    private fun stopPlayerKeepPrepared(mp: MediaPlayer) {
        runCatching {
            if (mp.isPlaying) mp.pause()
            mp.seekTo(0)
        }
    }
}

@Composable
fun rememberSpeaker(accent: Accent): (String) -> Unit {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
            }
        }
        tts = engine
        OgdenAudioHub.attachTts(engine)
        onDispose {
            OgdenAudioHub.attachTts(null)
            OgdenAudioHub.releaseWordPlayer()
            runCatching { engine.stop() }
            runCatching { engine.shutdown() }
        }
    }
    LaunchedEffect(accent, tts) {
        tts?.setBestLanguage(accent)
    }
    return remember(accent, tts, ready) {
        { rawText: String ->
            val text = rawText.trim()
            val engine = tts
            if (text.isEmpty()) return@remember
            val gen = OgdenAudioHub.beginWordPlay()

            fun fallbackTts() {
                if (!OgdenAudioHub.isWordPlayCurrent(gen)) return
                if (engine != null) {
                    engine.setBestLanguage(accent)
                    engine.setSpeechRate(0.82f)
                    engine.setPitch(1.04f)
                    val params = Bundle().apply {
                        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                    }
                    // QUEUE_FLUSH：打断上一段 TTS
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ogden-$gen")
                }
            }

            fun bindPlayer(mediaPlayer: MediaPlayer) {
                OgdenAudioHub.setWordPlayer(mediaPlayer, gen)
                if (!OgdenAudioHub.isWordPlayCurrent(gen)) return
                mediaPlayer.setOnPreparedListener { mp ->
                    if (!OgdenAudioHub.isWordPlayCurrent(gen)) {
                        runCatching { mp.release() }
                        OgdenAudioHub.clearWordPlayerIfSame(mp)
                        return@setOnPreparedListener
                    }
                    runCatching { mp.start() }
                }
                mediaPlayer.setOnCompletionListener { mp ->
                    runCatching { mp.release() }
                    OgdenAudioHub.clearWordPlayerIfSame(mp)
                }
                mediaPlayer.setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    OgdenAudioHub.clearWordPlayerIfSame(mp)
                    true
                }
            }

            fun playOnline() {
                if (!OgdenAudioHub.isWordPlayCurrent(gen)) return
                runCatching {
                    val mediaPlayer = MediaPlayer()
                    bindPlayer(mediaPlayer)
                    mediaPlayer.setOnErrorListener { mp, _, _ ->
                        runCatching { mp.release() }
                        OgdenAudioHub.clearWordPlayerIfSame(mp)
                        fallbackTts()
                        true
                    }
                    mediaPlayer.setDataSource(ogdenTtsUrl(text, accent))
                    mediaPlayer.prepareAsync()
                }.onFailure { fallbackTts() }
            }

            // 句型：phrases → examples → 在线/TTS；单词：audio/{us,uk} → 在线/TTS
            val candidates = listOfNotNull(
                localAudioPath(text, accent),
                localExampleAudioPath(text, accent)
            )
            if (candidates.isEmpty()) {
                playOnline()
            } else {
                fun tryAt(i: Int) {
                    if (i >= candidates.size) {
                        playOnline()
                        return
                    }
                    runCatching {
                        if (!OgdenAudioHub.isWordPlayCurrent(gen)) return@runCatching
                        val fd = context.assets.openFd(candidates[i])
                        val mediaPlayer = MediaPlayer()
                        bindPlayer(mediaPlayer)
                        mediaPlayer.setOnErrorListener { mp, _, _ ->
                            runCatching { mp.release() }
                            OgdenAudioHub.clearWordPlayerIfSame(mp)
                            tryAt(i + 1)
                            true
                        }
                        mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        fd.close()
                        mediaPlayer.prepareAsync()
                    }.onFailure { tryAt(i + 1) }
                }
                tryAt(0)
            }
        }
    }
}

internal fun audioSlug(text: String): String =
    text.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')

/**
 * 本地发音路径：
 * - 单词 → audio/{us,uk}/<slug>.mp3
 * - 非单词（句型/例句）→ 优先 audio/phrases/…（课本句型），再 audio/examples/…（词库例句）
 * openFd 失败时上层会回退在线/TTS。
 */
internal fun localAudioPath(text: String, accent: Accent): String? {
    val dir = if (accent == Accent.US) "us" else "uk"
    val slug = audioSlug(text)
    if (slug.isBlank()) return null
    // 单个单词：audio/{us,uk}/<word>.mp3
    if (text.matches(Regex("[A-Za-z][A-Za-z0-9-]*"))) {
        return "audio/$dir/$slug.mp3"
    }
    // 课本句型优先；词库例句次之（播放层 openFd 失败会回退）
    return "audio/phrases/$dir/$slug.mp3"
}

/** 句型路径失败时的例句回退路径（与 [localAudioPath] 同 slug）。 */
internal fun localExampleAudioPath(text: String, accent: Accent): String? {
    if (text.matches(Regex("[A-Za-z][A-Za-z0-9-]*"))) return null
    val dir = if (accent == Accent.US) "us" else "uk"
    val slug = audioSlug(text)
    if (slug.isBlank()) return null
    return "audio/examples/$dir/$slug.mp3"
}

// 答题反馈音效（中文、激励性），由 scripts/generate_sfx.py 生成到 assets/audio/sfx/
enum class SfxCue(val file: String) {
    Correct("correct"),
    Correct2("correct2"),
    Correct3("correct3"),
    Wrong("wrong"),
    Combo("combo"),
    Combo3("combo3"),
    Combo5("combo5"),
    Combo6("combo6"),
    Combo7("combo7"),
    Combo8("combo8"),
    Combo9("combo9"),
    Combo10("combo10"),
    /** 11 连及以上兜底，避免与「十连对」文案错位 */
    ComboMax("combo_max"),
    Perfect("perfect"),
    Complete("complete")
}

// 首答从三种表扬里随机挑；里程碑音效只在「恰好」该连击数播一次
private val CorrectCues = listOf(SfxCue.Correct, SfxCue.Correct2, SfxCue.Correct3)

/**
 * 连击 → 音效一一对应，禁止用数组取模把 9 映射成「十连对」。
 * 1 随机短表扬；2/3 里程碑；4 短表扬；5–10 专用；≥11 爆表。
 */
internal fun comboCue(combo: Int): SfxCue = when (combo) {
    2 -> SfxCue.Combo
    3 -> SfxCue.Combo3
    4 -> CorrectCues.random()
    5 -> SfxCue.Combo5
    6 -> SfxCue.Combo6
    7 -> SfxCue.Combo7
    8 -> SfxCue.Combo8
    9 -> SfxCue.Combo9
    10 -> SfxCue.Combo10
    else -> if (combo >= 11) SfxCue.ComboMax else CorrectCues.random()
}

@Composable
fun rememberSfx(): (SfxCue) -> Unit {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose { OgdenAudioHub.releaseAllSfx() }
    }
    return remember {
        { cue ->
            runCatching { OgdenAudioHub.playSfx(context, cue) }
        }
    }
}

internal fun ogdenTtsUrl(text: String, accent: Accent): String {
    val encoded = URLEncoder.encode(text, "UTF-8")
    val accentParam = if (accent == Accent.US) "us" else "uk"
    val rate = if (text.split(Regex("\\s+")).size <= 1) "+0%" else "-6%"
    return "https://ogden.munch.love/api/tts?text=$encoded&accent=$accentParam&rate=${URLEncoder.encode(rate, "UTF-8")}&v=android"
}

internal fun TextToSpeech.setBestLanguage(accent: Accent) {
    val result = setLanguage(accent.locale)
    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.ERROR) {
        setLanguage(Locale.ENGLISH)
    }
}
