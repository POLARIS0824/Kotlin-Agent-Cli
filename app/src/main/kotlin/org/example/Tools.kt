import kotlinx.serialization.json.*
import java.io.File

fun resolveSafe(workDir: File, path: String): File {
    val file = File(workDir, path).canonicalFile
    require(file.path == workDir.path || file.path.startsWith(workDir.path + File.separator)) {
        "路径超出工作目录: $path"
    }
    return file
}

fun createTools(workDir: File): List<Tool> = listOf(readFileTool(workDir), listFilesTool(workDir), editFileTool(workDir))

fun readFileTool(workDir: File) = Tool(
    name = "read_file",
    description = "读取指定路径文件的全部内容",
    inputSchema = buildJsonObject { 
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
                put("description", "文件的相对路径")
            }
        }
        putJsonArray("required") { add("path") }
    },
    execute = { args ->
            resolveSafe(workDir, args.str("path")).readText()
        },
)

fun listFilesTool(workDir: File) = Tool(
    name = "list_files",
    description = "列出目录下的文件和子目录，子目录名后带 /",
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
                put("description", "目录的相对路径，当前目录用 .")
            }
        }
        putJsonArray("required") { add("path") }
    },
    execute = { args ->
        val dir = resolveSafe(workDir, args.str("path"))
        dir.listFiles()
            ?.sortedBy { it.name }
            ?.joinToString("\n") { if (it.isDirectory) "${it.name}/" else it.name }
            ?: "目录不存在"
    },
)

fun editFileTool(workDir: File) = Tool(
    name = "edit_file",
    description = "把文件中的 old_str 替换为 new_str。old_str 必须在文件中唯一出现。" +
            "old_str 为空字符串时，创建新文件并写入 new_str。",
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
                put("description", "文件的相对路径")
            }
            putJsonObject("old_str") {
                put("type", "string")
                put("description", "要被替换的原文，必须唯一；创建新文件时传空字符串")
            }
            putJsonObject("new_str") {
                put("type", "string")
                put("description", "替换后的内容")
            }
        }
        putJsonArray("required") {
            add("path")
            add("old_str")
            add("new_str")
        }
    },
    execute = { args -> 
        val file = resolveSafe(workDir, args.str("path"))
        val old = args.str("old_str")
        val new = args.str("new_str")

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
    },
)