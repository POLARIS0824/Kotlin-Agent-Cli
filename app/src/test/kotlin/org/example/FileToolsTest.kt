import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FileToolsTest {
    @field:TempDir
    lateinit var workDir: File

    @Test
    fun `read tool returns UTF-8 file contents`() {
        File(workDir, "note.txt").writeText("你好 Kotlin\n第二行")

        val output = readFileTool(workDir).execute("""{"path":"note.txt"}""")

        assertEquals("你好 Kotlin\n第二行", output)
    }

    @Test
    fun `list tool sorts entries and marks directories`() {
        File(workDir, "b.txt").writeText("text")
        File(workDir, "a").mkdir()

        val output = listFilesTool(workDir).execute("""{"path":"."}""")

        assertEquals("a/\nb.txt", output)
    }

    @Test
    fun `empty old text creates a file and parent directories`() {
        val arguments = Json.encodeToString(EditFileArgs("nested/note.txt", "", "新文件"))

        editFileTool(workDir).execute(arguments)

        assertEquals("新文件", File(workDir, "nested/note.txt").readText())
    }

    @Test
    fun `a unique match is replaced while surrounding text is preserved`() {
        val file = File(workDir, "note.txt")
        file.writeText("first\n原文\nlast")
        val arguments = Json.encodeToString(EditFileArgs("note.txt", "原文", "新内容"))

        editFileTool(workDir).execute(arguments)

        assertEquals("first\n新内容\nlast", file.readText())
    }

    @Test
    fun `a missing match reports failure without changing the file`() {
        val file = File(workDir, "note.txt")
        file.writeText("keep this")
        val arguments = Json.encodeToString(EditFileArgs("note.txt", "missing", "replacement"))

        val output = editFileTool(workDir).execute(arguments)

        assertTrue(output.startsWith("错误："))
        assertEquals("keep this", file.readText())
    }

    @Test
    fun `multiple matches report failure without changing the file`() {
        val file = File(workDir, "note.txt")
        file.writeText("same and same")
        val arguments = Json.encodeToString(EditFileArgs("note.txt", "same", "replacement"))

        val output = editFileTool(workDir).execute(arguments)

        assertTrue(output.startsWith("错误："))
        assertEquals("same and same", file.readText())
    }

    @Test
    fun `missing edit argument fails before modifying the file`() {
        val file = File(workDir, "note.txt")
        file.writeText("keep this")

        assertFailsWith<SerializationException> {
            editFileTool(workDir).execute("""{"path":"note.txt","new_str":"replacement"}""")
        }

        assertEquals("keep this", file.readText())
    }

    @Test
    fun `invalid argument type fails before modifying the file`() {
        val file = File(workDir, "note.txt")
        file.writeText("keep this")

        assertFailsWith<SerializationException> {
            editFileTool(workDir).execute("""{"path":"note.txt","old_str":{},"new_str":"replacement"}""")
        }

        assertEquals("keep this", file.readText())
    }

    @Test
    fun `editing outside the working directory is rejected`() {
        val arguments = Json.encodeToString(EditFileArgs("../outside.txt", "", "replacement"))

        assertFailsWith<IllegalArgumentException> {
            editFileTool(workDir.canonicalFile).execute(arguments)
        }
    }
}
