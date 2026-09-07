package com.hrt.monitor.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI 兼容的 chat/completions 客户端。
 * 默认兼容 DeepSeek / OpenAI / 硅基流动 / Ollama 等；密钥仅由用户填写并只发给用户指定的服务。
 */
object AiClient {

    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        system: String,
        user: String
    ): String = withContext(Dispatchers.IO) {
        val base = baseUrl.trim().trimEnd('/')
        require(base.isNotEmpty()) { "接口地址为空" }
        require(apiKey.isNotBlank()) { "API 密钥为空" }
        require(model.isNotBlank()) { "模型名称为空" }

        val conn = URL(base + "/chat/completions").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 30_000
            conn.readTimeout = 180_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim())

            val body = JSONObject()
                .put("model", model.trim())
                .put("temperature", 0.3)
                .put("max_tokens", 2048)
                .put(
                    "messages", JSONArray()
                        .put(JSONObject().put("role", "system").put("content", system))
                        .put(JSONObject().put("role", "user").put("content", user))
                )
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw Exception("HTTP $code：${shortError(text)}")

            val root = JSONObject(text)
            root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } finally {
            conn.disconnect()
        }
    }

    private fun shortError(text: String): String = try {
        val msg = JSONObject(text).optJSONObject("error")?.optString("message")
        (msg?.takeIf { it.isNotBlank() } ?: text).take(200)
    } catch (_: Exception) {
        text.take(200)
    }
}
