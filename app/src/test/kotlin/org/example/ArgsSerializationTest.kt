import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArgsSerializationTest {
    @Test
    fun `read arguments decode a Chinese path`() {
        val args = Json.decodeFromString<ReadFileArgs>("""{"path":"笔记.txt"}""")

        assertEquals("笔记.txt", args.path)
    }

    @Test
    fun `edit arguments map JSON names to Kotlin properties`() {
        val args = Json.decodeFromString<EditFileArgs>(
            """{"path":"note.txt","old_str":"原文","new_str":"新内容"}"""
        )

        assertEquals(EditFileArgs("note.txt", "原文", "新内容"), args)
    }

    @Test
    fun `edit arguments encode protocol field names`() {
        val encoded = Json.encodeToString(EditFileArgs("note.txt", "before", "after"))

        assertEquals(
            Json.parseToJsonElement("""{"path":"note.txt","old_str":"before","new_str":"after"}"""),
            Json.parseToJsonElement(encoded)
        )
    }

    @Test
    fun `missing required path fails decoding`() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString<ReadFileArgs>("{}")
        }
    }

    @Test
    fun `an object cannot be used as a string path`() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString<ReadFileArgs>("""{"path":{"name":"note.txt"}}""")
        }
    }

    @Test
    fun `tool arguments reject unknown fields`() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString<ReadFileArgs>("""{"path":"note.txt","extra":true}""")
        }
    }
}
