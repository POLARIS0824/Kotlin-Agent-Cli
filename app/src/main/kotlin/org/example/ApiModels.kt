import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/*
    {
      "model": "你配置的模型名称",
      "messages": [
        {
          "role": "system",
          "content": "你是一个简洁的 Agent，可以利用工具读取、列出和编辑本地文件"
        },
        {
          "role": "user",
          "content": "读取 hello.txt"
        }
      ],
      "tools": [
        {
          "type": "function",
          "function": {
            "name": "read_file",
            "description": "读取指定路径文件的全部内容",
            "parameters": {
              "type": "object",
              "properties": {
                "path": {
                  "type": "string",
                  "description": "文件的相对路径"
                }
              },
              "required": ["path"]
            }
          }
        }
      ]
    }
*/
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition>? = null,
)

/*
    "tools": [
        {
            "type": "function",
            "function": {
                "name": "read_file",
                "description": "读取指定路径文件的内容",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "path": {
                            "type": "string",
                            "description": "文件的相对路径"
                        }
                    },`
                    "required": ["path"] // 非必需
                }
            }
        }
    ]
*/
@Serializable
data class ToolDefinition(
    val type: String,
    val function: FunctionDefinition,
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: ParameterSchema,
)

@Serializable
data class ParameterSchema(
    val type: String,
    val properties: Map<String, PropertySchema>,
    val required: List<String> = emptyList(),
)

@Serializable
data class PropertySchema(
    val type: String,
    val description: String
)

/*
    模型返回工具调用

    "choices": [
        {
            "message": {
                "role": "assistant",
                "content": null,
                "tool_calls": [
                    {
                        "id": "call_001",
                        "type": "function",
                        "function": {
                            "name": "read_file",
                            "arguments": "{\"path\":\"hello.txt\"}"
                        }
                    }
                ]
            }
        }
    ]

    对应：

    ChatResponse
    └─ choices: List<Choice>
       └─ message: ChatMessage
          ├─ role = "assistant"
          ├─ content = null
          └─ toolCalls: List<ToolCall>
             ├─ id = "call_001"
             ├─ type = "function"
             └─ function: FunctionCall
                ├─ name = "read_file"
                └─ arguments = "{\"path\":\"hello.txt\"}"
*/
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

// ParameterSchema：告诉 AI 需要一个 String 类型的 path
// ReadFileArgs：保存 AI 实际提供的 path 值
@Serializable
data class ReadFileArgs(
    val path: String
)

@Serializable
data class ListFileArgs(
    val path: String
)

@Serializable
data class EditFileArgs(
    val path: String,
    @SerialName("old_str")
    val oldStr: String,
    @SerialName("new_str")
    val newStr: String
)