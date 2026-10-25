fun prompt(label: String, default: String? = null): String {
    val hint = if (default != null) "[Default: $default]" else ""
    print("$label $hint: ")
    val line = readlnOrNull()?.trim().orEmpty()
    return line.ifEmpty { default ?: error("$label can't be empty!") }
}

fun main() {
    val apiKey = prompt("API Key")
    val baseUrl = prompt("Base URL", "https://api.ppio.com/openai")
    val model = prompt("Model", "deepseek/deepseek-v4.1-flash")

    val apiConfig = ApiConfig(apiKey = apiKey, baseUrl = baseUrl, model = model)
    val agentConfig = AgentConfig()

    val client = ApiClient(apiConfig)
    val tools = createTools(agentConfig.workDir)
    val agent = Agent(client, tools, agentConfig)

    println("\nAgent 已启动，工作目录：${agentConfig.workDir}")
    println("输入 exit 退出。")

    while (true) {
        print("\nYou: ")
        val input = readlnOrNull()?.trim() ?: break
        if (input == "exit") break
        if (input.isEmpty()) continue

        try {
            agent.chat(input)
        } catch (e: Exception) {
            println("\n[出错] ${e.message}")
        }
    }
}