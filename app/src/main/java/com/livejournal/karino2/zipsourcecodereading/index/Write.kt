package com.livejournal.karino2.zipsourcecodereading.index

import java.io.*

data class PostEntry(val trigram : Int, val fileId: Int) : Comparable<PostEntry> {
    override fun compareTo(other: PostEntry): Int =
            if (other.trigram != trigram)
                trigram.compareTo(other.trigram)
            else
                fileId.compareTo(other.fileId)
}

fun DataOutput.writeUtf8(s : String) {
    write(s.toByteArray(charset = Charsets.UTF_8))
}

fun DataOutput.writeTrigram(trigram: Int) {
    writeByte((trigram shr 16) and 0xff)
    writeByte((trigram shr 8) and 0xff)
    writeByte(trigram and 0xff)
}

fun DataOutput.writeVarint(n: Int) {
    var v = n
    while (v >= 0x80) {
        writeByte((v and 0x7f) or 0x80)
        v = v shr 7
    }
    writeByte(v)
}

class IndexException(message: String) : RuntimeException(message)

class IndexWriter(
        val tmpDir : File,
        outputFile: File,
        val postMax : Int = (64 shl 20) / 8 // 64MiB worth of posts entries
) {
    val paths = mutableListOf<String>()

    val nameDataFile = createTempFile("nameData")
    val nameData = DataOutputStream(BufferedOutputStream(FileOutputStream(nameDataFile)))
    val nameIndexFile = createTempFile("nameIndex")
    val nameIndex = DataOutputStream(BufferedOutputStream(FileOutputStream(nameIndexFile)))
    var numName = 0
    var totalBytes = 0L

    val posts = mutableListOf<PostEntry>()
    val postFiles = mutableListOf<File>()
    val postIndexFile = createTempFile("postIndex")

    val main = DataOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))

    private fun createTempFile(type: String) : File =
            File.createTempFile(type + "-", ".tmp", tmpDir)

    fun addPaths(paths : List<String>) {
        this.paths += paths
    }

    fun addFile(name: File) {
        FileInputStream(name).use {
            add(name.toString(), DataInputStream(BufferedInputStream(it)))
        }
    }

    fun add(name: String, inputStream: DataInput) {
        val trigrams = mutableSetOf<Int>()
        var tv = 0
        var fileLen = 0L
        var lineLen = 0
        try {
            while (true) {
                tv = (tv shl 8) and 0xffffff

                var c = inputStream.readByte()
                tv = tv or c.toUint()
                fileLen++
                if (fileLen >= 3) {
                    trigrams.add(tv)
                }
                if (!validUtf8((tv shr 8) and 0xff, tv and 0xff)) {
                    throw IndexException(String.format("%s: invalid UTF-8, ignoring", name))
                }

                if (fileLen > Const.MAX_FILE_LEN) {
                    throw IndexException(String.format("%s: too long, ignoring", name))
                }
                lineLen++
                if (lineLen > Const.MAX_LINE_LEN) {
                    throw IndexException(String.format("%s: very long lines, ignoring", name))
                }
                if (c == '\n'.toByte()) {
                    lineLen = 0
                }
            }
        } catch (e: EOFException) {
            // Ignore
        }

        if (trigrams.size > Const.MAX_TEXT_TRIGRAMS) {
            throw IndexException(String.format("%s: too many trigrams, probably not text, ignoring", name))
        }
        totalBytes += fileLen

        val fileId = addName(name)

            // TODO: Log

        for (t in trigrams) {
            if (posts.size >= postMax) {
                flushPost()
            }
            posts += PostEntry(t, fileId)
        }
    }

    private fun addName(name: String) : Int {
        nameIndex.writeInt(nameData.size())
        nameData.writeUtf8(name)
        nameData.writeByte(0)
        return numName++
    }

    // flushes the index entry to the target file.
    fun flush() {
        addName("")

        nameData.close()
        nameIndex.close()

        val offs = mutableListOf<Int>()

        main.writeUtf8(Const.MAGIC)
        offs += main.size()
        for (s in paths) {
            main.writeUtf8(s)
            main.write(0)
        }
        main.write(0)
        offs += main.size()
        copyFile(main, nameDataFile)
        offs += main.size()
        mergePost(main)
        offs += main.size()
        copyFile(main, nameIndexFile)
        offs += main.size()
        copyFile(main, postIndexFile)

        for (off in offs) {
            main.writeInt(off)
        }
        main.writeUtf8(Const.TRAILER_MAGIC)
        main.close()

        nameDataFile.delete()
        nameIndexFile.delete()
        postIndexFile.delete()
        postFiles.forEach { it.delete() }
    }

    private fun copyFile(dest: OutputStream, src: File) {
        val buf = ByteArray(1 shl 14)
        src.inputStream().use {
            while (true) {
                val n = it.read(buf)
                if (n <= 0) break
                dest.write(buf, 0, n)
            }
        }
    }

    private fun flushPost() {
        val tempIndexFile = File.createTempFile("postEntry-", ".idx", tmpDir)
        posts.sortBy { it.trigram }
        DataOutputStream(BufferedOutputStream(tempIndexFile.outputStream())).use {
            out ->
            for (post in posts) {
                out.writeInt(post.trigram)
                out.writeInt(post.fileId)
            }
        }
        this.posts.clear()
        postFiles += tempIndexFile
    }

    private fun mergePost(out: DataOutputStream) {
        val heap = PostHeap()

        for (f in postFiles) {
            val entries = FiledPostEntries(f)
            if (entries.hasNext()) {
                heap.add(PostChunk(entries))
            }
        }
        if (posts.isNotEmpty()) {
            posts.sortBy { it.trigram }
            heap.add(PostChunk(posts.iterator()))
        }

        DataOutputStream(BufferedOutputStream(FileOutputStream(postIndexFile))).use {
            postIndex ->

            var nPost = 0
            val offset0 = out.size()
            var entry = heap.next()
            do {
                nPost++
                val offset = out.size() - offset0
                val trigram = entry.trigram

                out.writeTrigram(trigram)

                var fileId = -1
                var nFile = 0;

                do {
                    out.writeVarint(entry.fileId - fileId)
                    fileId = entry.fileId
                    nFile++
                    if (!heap.hasNext()) break;
                    entry = heap.next()
                } while (entry.trigram == trigram)
                out.writeVarint(0)

                postIndex.writeTrigram(trigram)
                postIndex.writeInt(nFile)
                postIndex.writeInt(offset)
            } while (heap.hasNext())
        }
    }
}

class FiledPostEntries(val stream: DataInputStream) : Iterator<PostEntry> {
    var next : PostEntry? = prepareNext()

    constructor(file: File) :
            this(DataInputStream(BufferedInputStream(FileInputStream(file)))) {
    }

    override fun hasNext(): Boolean = next != null

    override fun next(): PostEntry {
        val ret = next!!
        prepareNext()
        return ret
    }

    private fun prepareNext(): PostEntry? {
        try {
            val trigram = stream.readInt()
            val fileId = stream.readInt()
            next = PostEntry(trigram = trigram, fileId = fileId)
        } catch (e: EOFException) {
            next = null
        }
        return next
    }
}

class PostChunk(val m: Iterator<PostEntry>) : Comparable<PostChunk> {
    var e: PostEntry = m.next()
    override fun compareTo(other: PostChunk) = e.compareTo(other.e)
}

class PostHeap : Iterator<PostEntry> {
    private val chunks = java.util.TreeSet<PostChunk>()

    fun add(ch: PostChunk) = chunks.add(ch)

    override fun next() : PostEntry {
        val ch = chunks.pollFirst()
        val entry = ch.e
        if (ch.m.hasNext()) {
            ch.e = ch.m.next()
            chunks.add(ch)
        }
        return entry
    }

    override fun hasNext() : Boolean = !chunks.isEmpty()
}

fun validUtf8(c1: Int, c2: Int) : Boolean =
        when (c1) {
            in 0 until 0x80 ->
                // 1-byte, must be followed by 1-byte or first of multi-byte
                c2 < 0x80 || c2 in 0xc0 until 0xf8
            in 0x80 until 0xc0 ->
                // continuation byte, can be followed by nearly anything
                c2 < 0xf8
            in 0xc0 until 0xf8 ->
                // first of multi-byte, must be followed by continuation byte
                c2 in 0x80 until 0xc0
            else ->
                false
        }