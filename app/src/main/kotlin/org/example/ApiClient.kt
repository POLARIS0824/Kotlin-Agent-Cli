import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class ApiClient(private val config: ApiConfig) {
    private val http = HttpClient.newHttpClient()

    private val json = Json { 
        ignoreUnknownKeys = true
    }

    fun send(
        messages: List<ChatMessage>,
        systemPrompt: String,
        tools: List<AgentTool>,
    ): ChatResponse {
        // 请求正文。OpenAI 格式里 system 提示词是 role=system 的第一条消息，不是顶层字段
        val body = ChatRequest(
            model = config.model,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = systemPrompt,
                )
            ) + messages,
            tools = tools
                .takeIf { it.isNotEmpty() }
                ?.map { it.toDefinition() },
        )

        // HTTP 请求
        val request = HttpRequest.newBuilder(URI(config.url))
            .timeout(Duration.ofSeconds(120))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                // 序列化
                json.encodeToString(body)
            ))
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("API 请求失败 HTTP ${response.statusCode()}: ${response.body().take(500)}")
        }

        // 反序列化
        return json.decodeFromString<ChatResponse>(response.body())
    }
}
