import kotlinx.serialization.json.*

class Agent (
    private val client: ApiClient,
    private val tools: List<AgentTool>,
    private val config: AgentConfig 
) {
    // 对话历史
    private val messages = mutableListOf<ChatMessage>()

    private val json = Json { 
        ignoreUnknownKeys = true
    }

    // 处理一次用户输入
    fun chat(userInput: String) {
        messages += ChatMessage(
            role = "user",
            content = userInput
        )

        for (i in 1..config.maxIterations) {
            val resp = client.send(messages, config.systemPrompt, tools)
            val message = resp.choices.firstOrNull()?.message
                ?: error("API 返回的 choices 为空")
                
            messages += message

            val text = message.content
            if (!text.isNullOrBlank()) println("\nAI: $text")

            val toolCalls = message.toolCalls
            if (toolCalls.isNullOrEmpty()) return

            for (call in toolCalls) {
                messages += runToolCall(call)
            }
        }
        println("\n[已达到最大轮数 ${config.maxIterations}，已停止]")
    }

    // 执行一个工具调用
    private fun runToolCall(call: ToolCall): ChatMessage {
        val id = call.id
        val function = call.function
        val name = call.function.name
        val argsText = call.function.arguments
        println("  [工具] $name $argsText")

        val output = try {
            val tool = tools.firstOrNull { it.name == name } 
                ?: error("未知工具: $name")
            
            tool.execute(argsText)
        } catch (e: Exception) {
            "错误：${e.message}"
        }
    
        return ChatMessage(
            role = "tool",
            content = output,
            toolCallId = id
        )
    }
}