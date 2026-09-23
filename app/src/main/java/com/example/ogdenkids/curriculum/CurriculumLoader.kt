package com.example.ogdenkids.curriculum

import android.content.Context
import com.example.ogdenkids.Category
import com.example.ogdenkids.OgdenWord
import org.json.JSONArray
import org.json.JSONObject

fun parseCurriculumIndex(json: String): CurriculumIndex {
    val root = JSONObject(json.trimStart('\uFEFF'))
    val volumes = root.optJSONArray("volumes") ?: JSONArray()
    return CurriculumIndex(
        grade = root.optInt("grade"),
        volumes = List(volumes.length()) { i ->
            val item = volumes.getJSONObject(i)
            CurriculumVolume(
                volume = item.optInt("volume"),
                titleZh = item.optString("titleZh"),
                unitIds = item.optJSONArray("unitIds").toStringList()
            )
        }
    )
}

fun parseCurriculumVolume(json: String): List<CurriculumUnit> {
    val arr = JSONArray(json.trimStart('\uFEFF'))
    return List(arr.length()) { i ->
        val item = arr.getJSONObject(i)
        val phrases = item.optJSONArray("phrases") ?: JSONArray()
        val activities = item.optJSONArray("activities") ?: JSONArray()
        CurriculumUnit(
            id = item.getString("id"),
            grade = item.optInt("grade"),
            volume = item.optInt("volume"),
            unit = item.optInt("unit"),
            titleEn = item.optString("titleEn"),
            titleZh = item.optString("titleZh"),
            goalsZh = item.optJSONArray("goalsZh").toStringList(),
            words = item.optJSONArray("words").toStringList(),
            phrases = List(phrases.length()) { p ->
                val o = phrases.getJSONObject(p)
                CurriculumPhrase(o.optString("en"), o.optString("zh"))
            },
            activities = List(activities.length()) { a ->
                val o = activities.getJSONObject(a)
                CurriculumActivity(o.optString("type"), o.optString("titleZh"), o.optString("bodyZh"))
            },
            weeksHint = (item.optJSONArray("weeksHint") ?: JSONArray()).let { w ->
                List(w.length()) { w.optInt(it) }
            }
        )
    }
}

/** 补词固定归 Topics，只为配色；不参与 Ogden 分类计数。可选 `ipa: {uk,us}`。 */
fun parsePepExtraWords(json: String): List<OgdenWord> {
    val arr = JSONArray(json.trimStart('\uFEFF'))
    return List(arr.length()) { i ->
        val item = arr.getJSONObject(i)
        val ipa = item.optJSONObject("ipa")
        OgdenWord(
            word = item.getString("w"),
            category = Category.Topics,
            zh = item.optString("zh"),
            englishDefinition = item.optString("en"),
            example = item.optString("ex"),
            exampleZh = item.optString("exz"),
            synonyms = item.optJSONArray("s").toStringList(),
            ipaUk = ipa?.optString("uk").orEmpty(),
            ipaUs = ipa?.optString("us").orEmpty()
        )
    }
}

/**
 * 课本练习词典：Ogden 在前、extra 在后。
 * 同名词以 extra 的释义/例句/近义词为准，但保留 Ogden 的 IPA（extra 有 IPA 时优先用 extra）。
 */
fun mergePracticeDictionary(
    ogdenWords: List<OgdenWord>,
    extraWords: List<OgdenWord>
): Map<String, OgdenWord> {
    val ogdenByKey = ogdenWords.associateBy { it.word }
    val merged = LinkedHashMap<String, OgdenWord>(ogdenByKey.size + extraWords.size)
    ogdenByKey.forEach { (k, v) -> merged[k] = v }
    extraWords.forEach { extra ->
        val base = ogdenByKey[extra.word]
        merged[extra.word] = if (base == null) {
            extra
        } else {
            extra.copy(
                ipaUk = extra.ipaUk.ifBlank { base.ipaUk },
                ipaUs = extra.ipaUs.ifBlank { base.ipaUs }
            )
        }
    }
    return merged
}

/** 支持的人教版年级：三～六 */
val PEP_SUPPORTED_GRADES: List<Int> = listOf(3, 4, 5, 6)

fun loadPepCurriculum(context: Context, grade: Int): CurriculumBundle {
    val g = if (grade in PEP_SUPPORTED_GRADES) grade else 3
    fun read(name: String) =
        context.assets.open("curriculum/$name").bufferedReader().use { it.readText() }
    return CurriculumBundle(
        index = parseCurriculumIndex(read("pep_grade${g}_index.json")),
        units = parseCurriculumVolume(read("pep_grade${g}_vol1.json")) +
            parseCurriculumVolume(read("pep_grade${g}_vol2.json")),
        extraWords = parsePepExtraWords(read("pep_extra_words.json"))
    )
}

/** @deprecated 用 [loadPepCurriculum]；保留以免旧调用方编译失败 */
fun loadGrade3Curriculum(context: Context): CurriculumBundle = loadPepCurriculum(context, 3)

fun resolveUnitWords(unit: CurriculumUnit, dictionary: Map<String, OgdenWord>): ResolvedUnitWords {
    val words = mutableListOf<OgdenWord>()
    val missing = mutableListOf<String>()
    unit.words.forEach { key ->
        val hit = dictionary[key]
        if (hit == null) missing += key else words += hit
    }
    return ResolvedUnitWords(words, missing)
}

private fun JSONArray?.toStringList(): List<String> =
    if (this == null) emptyList() else List(length()) { optString(it) }
