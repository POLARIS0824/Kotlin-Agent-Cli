import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import java.io.File

fun resolveSafe(workDir: File, path: String): File {
    val file = File(workDir, path).canonicalFile
    require(file.path == workDir.path || file.path.startsWith(workDir.path + File.separator)) {
        "路径超出工作目录: $path"
    }
    return file
}

/** 工厂函数 */
fun createTools(workDir: File): List<AgentTool> = listOf(
    readFileTool(workDir), 
    listFilesTool(workDir),
    editFileTool(workDir)
)

fun readFileTool(workDir: File) = Tool(
    name = "read_file",
    description = "读取指定路径文件的全部内容",
    inputSchema = ParameterSchema(
        type = "object",
        properties = mapOf(
            "path" to PropertySchema(
                type = "string",
                description = "文件的相对路径"
            )
        ),
        required = listOf("path")
    ),
    
    serializer = ReadFileArgs.serializer(),

    // 这里是闭包：闭包就是一个函数，以及它捕获的外部作用域中的变量所形成的组合
    // 
    // 这个 lambda 参数只有 args，但是函数体里面调用了 workDir
    // 这个 lambda 捕获了 workDir，这就构成了闭包
    // 
    // 一个 Lambda 不仅能使用自己的参数和局部变量，还能访问定义它时所在作用域的变量
    // 即使执行时已经离开原来的作用域，仍然可以使用这些被捕获的变量，这就是闭包的核心特征
    action = { args: ReadFileArgs ->
        resolveSafe(workDir, args.path).readText()
    },
)

fun listFilesTool(workDir: File) = Tool(
    name = "list_files",
    description = "列出目录下的文件和子目录，子目录名后带 /",
    inputSchema = ParameterSchema(
        type = "object",
        properties = mapOf(
            "path" to PropertySchema(
                type = "string",
                description = "目录的相对路径，当前目录用 ."
            )
        ),
        required = listOf("path")
    ),

    serializer = ListFileArgs.serializer(),

    action = { args: ListFileArgs ->
        val dir = resolveSafe(workDir, args.path)
        
        dir.listFiles()
            ?.sortedBy { it.name }
            ?.joinToString("\n") {
                if (it.isDirectory) "${it.name}/"
                else it.name
            }
            ?: "目录不存在"
    }
)

fun editFileTool(workDir: File) = Tool(
    name = "edit_file",
    description = "把文件中的 old_str 替换为 new_str。old_str 必须在文件中唯一出现。" +
            "old_str 为空字符串时，创建新文件并写入 new_str。",
    inputSchema = ParameterSchema(
        type = "object",
        properties = mapOf(
            "path" to PropertySchema(
                type = "string",
                description = "文件的相对路径"
            ),
            "old_str" to PropertySchema(
                type = "string",
                description = "要被替换的原文，必须唯一；创建新文件时传空字符串"
            ),
            "new_str" to PropertySchema(
                type = "string",
                description = "替换后的内容"
            )
        ),
        required = listOf("path", "old_str", "new_str")
    ),

    serializer = EditFileArgs.serializer(),

    action = { args: EditFileArgs ->
        val file = resolveSafe(workDir, args.path)
        val old = args.oldStr
        val new = args.newStr

        if (old.isEmpty()) {
            file.parentFile?.mkdirs()
            file.writeText(new)
            "已写入 ${file.name}"
        } else {
            val text = file.readText()
            val count = text.split(old).size - 1
            when (count) {
                0 -> "错误：没有找到 old_str"
                1 -> {
                    file.writeText(text.replace(old, new))
                    "已编辑 ${file.name}"
                }
                else -> "错误：old_str 出现了 $count 次，请提供更长的上下文使其唯一"
            }
        }
    }
    // execute = { args -> 
    //     val file = resolveSafe(workDir, args.str("path"))
    //     val old = args.str("old_str")
    //     val new = args.str("new_str")

    //     if (old.isEmpty()) {
    //         file.parentFile?.mkdirs()
    //         file.writeText(new)
    //         "已写入 ${file.name}"
    //     } else {
    //         val text = file.readText()
    //         val count = text.split(old).size - 1
    //         when (count) {
    //             0 -> "错误：没有找到 old_str"
    //             1 -> {
    //                 file.writeText(text.replace(old, new))
    //                 "已编辑 ${file.name}"
    //             }
    //             else -> "错误：old_str 出现了 $count 次，请提供更长的上下文使其唯一"
    //         }
    //     }
    // },
)