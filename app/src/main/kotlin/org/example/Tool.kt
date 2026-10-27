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

    override fun execute(arguments: String): String {
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