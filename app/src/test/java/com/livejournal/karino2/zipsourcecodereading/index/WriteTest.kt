package com.livejournal.karino2.zipsourcecodereading.index

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
        indexWriter.add("abc.txt", createTestStream("abc"))

        assertEquals(1, indexWriter.paths.size)

        assertEquals(1, indexWriter.numName)
        assertEquals(1, indexWriter.posts.size)
        assertEquals("abc".trigram(), indexWriter.posts[0].trigram)

        indexWriter.add("bcd.txt", createTestStream("bcdf"))
        indexWriter.addPaths(arrayListOf("bcd.txt"))

        assertEquals(2, indexWriter.numName)
        assertEquals(3, indexWriter.posts.size)

        indexWriter.flush()
    }

    @Test
    fun IndexWriter_manyPostFiles() {
        val indexWriter = IndexWriter(tmpDir, tmpOutput, postMax = 4)

        indexWriter.addPaths(arrayListOf("9.txt"))
        indexWriter.add("9.txt", createTestStream("123456789--"))
        assertEquals(2, indexWriter.postFiles.size)
        assertEquals(1, indexWriter.posts.size)

        indexWriter.flush()
    }

    private fun createTestStream(s : String) : DataInputStream =
        DataInputStream(ByteArrayInputStream(s.toByteArray()))

    @Test
    fun FiledPostEntries_empty() {
        val entries = FiledPostEntries(createTestStream(""))

        assertFalse(entries.hasNext())
    }

    @Test
    fun FiledPostEntries_one() {
        val entries = FiledPostEntries(DataInputStream(ByteArrayInputStream(
                byteArrayOf(0,0,0,1,0,0,0,2))))

        assertTrue(entries.hasNext())
        val entry = entries.next()
        assertEquals(1, entry.trigram)
        assertEquals(2, entry.fileId)
        assertFalse(entries.hasNext())
    }
}
