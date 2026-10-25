import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class ApiClient(private val config: ApiConfig) {
    private val http = HttpClient.newHttpClient()

    fun send(
        messages: List<JsonObject>,
        systemPrompt: String,
        tools: List<Tool>,
    ): JsonObject {
        // 请求正文。OpenAI 格式里 system 提示词是 role=system 的第一条消息，不是顶层字段
        val body = buildJsonObject {
            put("model", config.model)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                messages.forEach { add(it) }
            }
            if (tools.isNotEmpty()) {
                putJsonArray("tools") { tools.forEach { add(it.toApiJson()) } }
            }
        }

        // HTTP 请求
        val request = HttpRequest.newBuilder(URI(config.url))
            .timeout(Duration.ofSeconds(120))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("API 请求失败 HTTP ${response.statusCode()}: ${response.body().take(500)}")
        }

        return runCatching { Json.parseToJsonElement(response.body()).jsonObject }
            .getOrElse { error("API 返回了非 JSON 内容: ${response.body().take(200)}") }
    }
}
