import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlin.reflect.typeOf

/* 示例：tools 数组里的一项（OpenAI 格式）
    {
      "type": "function",
      "function": {
        "name": "read_file",
        "description": "读取指定路径文件的全部内容",
        // 用来描述参数
        "parameters": {
          "type": "object",
          "properties": {
            "path": { "type": "string", "description": "文件的相对路径" }
          },
          "required": ["path"]
        }
      }
    }
*/

interface AgentTool {
    val name: String
    fun execute(arguments: String): String
    fun toDefinition(): ToolDefinition
}

class Tool<T>(
    // 给模型的菜单
    override val name: String,
    val description: String,
    val inputSchema: ParameterSchema,
    private val serializer: KSerializer<T>,
    private val action: (T) -> String,
) : AgentTool {

    /**
     * execute 负责如何运行工具 | Agent 调用 | 接受 JSON 字符串 | 由 Tool 统一实现
     * 
     * action 负责这个工具具体做什么 | execute 调用 | 接受解码后的参数对象 | 创建每个工具时传入具体实现
     */
    override fun execute(arguments: String): String {
        // 在这里把 arguments 转为 Args
        val args = Json.decodeFromString(serializer, arguments)
        return action(args)
    }

    // override 表示这个属性或方法是在实现接口中的声明
    override fun toDefinition(): ToolDefinition = ToolDefinition(
        type = "function",
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = inputSchema
        )
    )
}