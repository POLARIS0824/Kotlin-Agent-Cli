import java.io.File

data class ApiConfig(
    val baseUrl: String,
    val endpoint: String = "/v1/chat/completions",
    val apiKey: String,
    val model: String,
) {
    val url: String
        get() = "${baseUrl.trimEnd('/')}/${endpoint.trimStart('/')}"
}

data class AgentConfig(
    val systemPrompt: String = "你是一个简洁的 Agent, 可以利用工具读取, 列出和编辑本地文件",
    val workDir: File = File(".").canonicalFile,
    val maxIterations: Int = 20,
)