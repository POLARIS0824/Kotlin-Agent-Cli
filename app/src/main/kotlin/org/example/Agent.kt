import kotlinx.serialization.json.*

class Agent (
    private val client: ApiClient,
    private val tools: List<Tool>,
    private val config: AgentConfig 
) {
    // 对话历史
    private val messages = mutableListOf<JsonObject>()

    // 处理一次用户输入
    fun chat(userInput: String) {
        messages += buildJsonObject { 
            put("role", "user")
            put("content", userInput)
        }

        for (i in 1..config.maxIterations) {
            val resp = client.send(messages, config.systemPrompt, tools)
            val message = resp["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject
            messages += message

            val text = message["content"]?.jsonPrimitive?.contentOrNull
            if (!text.isNullOrBlank()) println("\nAI: $text")

            val toolCalls = message["tool_calls"]?.jsonArray
            if (toolCalls.isNullOrEmpty()) return

            for (call in toolCalls) {
                messages += runToolCall(call.jsonObject)
            }
        }
        println("\n[已达到最大轮数 ${config.maxIterations}，已停止]")
    }

    // 执行一个工具调用
    private fun runToolCall(call: JsonObject): JsonObject {
        val id = call["id"]!!.jsonPrimitive.content
        val function = call["function"]!!.jsonObject
        val name = function["name"]!!.jsonPrimitive.content
        val argsText = function["arguments"]?.jsonPrimitive?.contentOrNull ?: ""
        println("  [工具] $name $argsText")

        val output = try {
            val tool = tools.firstOrNull { it.name == name } ?: error("未知工具: $name")
            val args = Json.parseToJsonElement(argsText.ifBlank { "{}" }).jsonObject
            tool.execute(args)
        } catch (e: Exception) {
            "错误：${e.message}"
        }
    
        return buildJsonObject {
            put("role", "tool")
            put("tool_call_id", id)
            put("content", output)
        }
    }
}