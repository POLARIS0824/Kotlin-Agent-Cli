import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition>? = null,
)

@Serializable
data class ToolDefinition(
    val function: FunctionDefinition,
    val type: String,
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>,
)

@Serializable
data class Choice(
    val message: ChatMessage,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,

    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,

    @SerialName("tool_call_id")
    val toolCallId: String? = null,
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String,
    val function: FunctionCall,
)

@Serializable
data class FunctionCall(
    val name: String,
    // API 在这里返回 JSON 字符串，暂时保持 String
    val arguments: String,
)