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

class Tool(
    // 给模型的菜单
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    // 真正执行的函数
    val execute: (JsonObject) -> String,
) {
    fun toDefinition(): ToolDefinition = ToolDefinition(
        type = "function",
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = inputSchema,
        )
    )
}

/** 从参数里取字符串，缺失时给出清楚的错误信息 */
// 拓展函数
fun JsonObject.str(key: String): String =
    this[key]?.jsonPrimitive?.content ?: error("缺少参数: $key")

