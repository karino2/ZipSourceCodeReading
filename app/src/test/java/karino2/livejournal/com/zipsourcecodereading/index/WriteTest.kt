package karino2.livejournal.com.zipsourcecodereading.index

import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import org.junit.Assert.*

class WriteTest {
    val tmpDir = File("/tmp")
    val tmpOutput = File(tmpDir, "out")

    @Test
    fun IndexWriter_oneTrigram() {
        val indexWriter = IndexWriter(tmpDir, tmpOutput)
        indexWriter.addPaths(arrayListOf("abc.txt"))
        indexWriter.add("abc.txt", DataInputStream(ByteArrayInputStream("abc".toByteArray())))

        assertEquals(1, indexWriter.paths.size)
        assertEquals(1, indexWriter.trigrams.size)
        assertTrue(indexWriter.trigrams.contains("abc".trigram()))

        assertEquals(1, indexWriter.numName)
        assertEquals(1, indexWriter.posts.size)

        indexWriter.add("bcd.txt", DataInputStream(ByteArrayInputStream("bcdf".toByteArray())))
        indexWriter.addPaths(arrayListOf("bcd.txt"))

        assertEquals(2, indexWriter.trigrams.size)
        assertTrue(indexWriter.trigrams.contains("bcd".trigram()))
        assertEquals(2, indexWriter.numName)
        assertEquals(3, indexWriter.posts.size)

        indexWriter.flush()
    }
}
