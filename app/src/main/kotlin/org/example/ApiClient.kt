import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.streams.toList

class ApiClient(private val config: ApiConfig, private val httpClient: HttpClient) {

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
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    // 序列化
                    json.encodeToString(body)
                )
            )
            .build()

        val response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        )
        if (response.statusCode() !in 200..299) {
            error("API 请求失败 HTTP ${response.statusCode()}: ${response.body().take(500)}")
        }

        // 反序列化
        return json.decodeFromString<ChatResponse>(response.body())
    }

    fun streamText(
        messages: List<ChatMessage>,
        systemPrompt: String,
        tools: List<AgentTool>,
    ): ChatResponse {
        require(tools.isEmpty()) {
            "暂不支持 Tool Calling"
        }

        val fullText = StringBuilder()
        var completed = false
        
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
            stream = true,
        )

        val request = HttpRequest.newBuilder(URI(config.url))
            .timeout(Duration.ofSeconds(120))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    // 序列化
                    json.encodeToString(body)
                )
            )
            .build()

        val response = httpClient.send(
            request,
            // SSE: 从 ofString() 变为 ofLines(), 返回 Stream<String>
            HttpResponse.BodyHandlers.ofLines()
        )

        response.body().use { lines ->
            if (response.statusCode() !in 200..299) {
                error("HTTP ${response.statusCode()}")
            }
        
            for (line in lines.iterator()) {
                if (!line.startsWith("data:")) continue
        
                val data = line.removePrefix("data:").trimStart()
        
                if (data == "[DONE]") {
                    completed = true
                    break
                }
        
                val chunk = json.decodeFromString<StreamChunk>(data)
                val content = chunk.choices.firstOrNull()?.delta?.content
        
                if (content != null) {
                    fullText.append(content)
                    print(content)
                    System.out.flush()
                }
            }
        }
        
        check(completed) {
            "SSE 流提前结束，没有收到 [DONE]"
        }
        
        println()
        
        return ChatResponse(
            choices = listOf(
                Choice(
                    message = ChatMessage(
                        role = "assistant",
                        content = fullText.toString()
                    )
                )
            )
        )
    }
}
