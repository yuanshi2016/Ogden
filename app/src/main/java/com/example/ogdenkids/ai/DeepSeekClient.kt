package com.example.ogdenkids.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

// DeepSeek OpenAI 兼容接口的最小客户端：文本对话 + 图片问答。
// 只依赖 JDK HttpURLConnection 与 Android 内置 org.json，不引入网络库。
object DeepSeekClient {
    private const val BASE_URL = "https://api.deepseek.com/chat/completions"
    private const val CHAT_MODEL = "deepseek-flash"
    private const val VISION_MODEL = "deepseek-v4-flash-vision-exp"

    // 口语陪练系统提示：限定用 850 基础词、短句，第二行给中文翻译
    val TUTOR_SYSTEM_PROMPT = """
        You are a friendly English tutor chatting with a Chinese child who is learning English.
        Reply in very simple English, using mostly Ogden Basic English 850 words. Keep it to 1-3 short sentences.
        Be encouraging and ask one question at a time.
        On a new line, give the Chinese translation of your reply in parentheses, like:
        (中文翻译)
    """.trimIndent()

    private val VISION_PROMPT = """
        你是一位耐心的家教老师，学生在做作业时遇到了困难。
        请看看这张图片里的题目：如果题目是中文，就用简体中文回答；如果题目是英文，就用简单英文回答。
        讲解思路，引导孩子自己得出答案，不要直接给出最终答案。
        控制在 200 字以内，语气亲切。
    """.trimIndent()

    class DeepSeekException(message: String) : Exception(message)

    // messages: 交替的 (role, content) 对，role 为 "user" / "assistant" / "system"
    // focusAddon: 可选，把待复习词附加到 system prompt，让陪练贴合学习进度
    suspend fun chat(
        apiKey: String,
        messages: List<Pair<String, String>>,
        focusAddon: String = ""
    ): String = withContext(Dispatchers.IO) {
        val system = if (focusAddon.isBlank()) TUTOR_SYSTEM_PROMPT
        else "$TUTOR_SYSTEM_PROMPT\n\n$focusAddon"
        val body = JSONObject().apply {
            put("model", CHAT_MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                messages.forEach { (role, content) ->
                    put(JSONObject().put("role", role).put("content", content))
                }
            })
            put("temperature", 0.7)
            put("max_tokens", 300)
        }
        post(apiKey, body, readTimeoutMs = 60_000)
    }

    suspend fun describeImage(apiKey: String, imageBytes: ByteArray, mime: String): String = withContext(Dispatchers.IO) {
        val dataUri = "data:$mime;base64," + android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
        val body = JSONObject().apply {
            put("model", VISION_MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "user").put("content", JSONArray().apply {
                    put(JSONObject().put("type", "text").put("text", VISION_PROMPT))
                    put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", dataUri)))
                }))
            })
            put("temperature", 0.3)
            put("max_tokens", 500)
        }
        post(apiKey, body, readTimeoutMs = 90_000)
    }

    private fun post(apiKey: String, body: JSONObject, readTimeoutMs: Int): String {
        val connection = URL(BASE_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = readTimeoutMs
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream: InputStream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val message = runCatching {
                    val error = JSONObject(raw).optJSONObject("error")
                    error?.optString("message")?.takeIf { it.isNotBlank() } ?: "HTTP $code"
                }.getOrDefault("HTTP $code")
                throw DeepSeekException(message)
            }
            return JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } finally {
            connection.disconnect()
        }
    }
}
