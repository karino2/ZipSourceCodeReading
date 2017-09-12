package karino2.livejournal.com.zipsourcecodereading.index

import karino2.livejournal.com.zipsourcecodereading.Query
import karino2.livejournal.com.zipsourcecodereading.QueryOp
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

// Copied From https://github.com/google/codesearch

// An index stored on disk has the format:
//
//	"csearch index 1\n"
//	list of paths
//	list of names
//	list of posting lists
//	name index
//	posting list index
//	trailer
//
// The list of paths is a sorted sequence of NUL-terminated file or directory names.
// The index covers the file trees rooted at those paths.
// The list ends with an empty name ("\x00").
//
// The list of names is a sorted sequence of NUL-terminated file names.
// The initial entry in the list corresponds to file #0,
// the next to file #1, and so on.  The list ends with an
// empty name ("\x00").
//
// The list of posting lists are a sequence of posting lists.
// Each posting list has the form:
//
//	trigram [3]
//	deltas [v]...
//
// The trigram gives the 3 byte trigram that this list describes.  The
// delta list is a sequence of varint-encoded deltas between file
// IDs, ending with a zero delta.  For example, the delta list [2,5,1,1,0]
// encodes the file ID list 1, 6, 7, 8.  The delta list [0] would
// encode the empty file ID list, but empty posting lists are usually
// not recorded at all.  The list of posting lists ends with an entry
// with trigram "\xff\xff\xff" and a delta list consisting a single zero.
//
// The indexes enable efficient random access to the lists.  The name
// index is a sequence of 4-byte big-endian values listing the byte
// offset in the name list where each name begins.  The posting list
// index is a sequence of index entries describing each successive
// posting list.  Each index entry has the form:
//
//	trigram [3]
//	file count [4]
//	offset [4]
//
// Index entries are only written for the non-empty posting lists,
// so finding the posting list for a specific trigram requires a
// binary search over the posting list index.  In practice, the majority
// of the possible trigrams are never seen, so omitting the missing
// ones represents a significant storage savings.
//
// The trailer has the form:
//
//	offset of path list [4]
//	offset of name list [4]
//	offset of posting lists [4]
//	offset of name index [4]
//	offset of posting list index [4]
//	"\ncsearch trailr\n"

object Const {
    const val MAGIC = "csearch index 1\n"
    const val TRAILER_MAGIC = "\ncsearch trailr\n"

    const val POST_ENTRY_SIZE = 3 + 4 + 4
}

fun Byte.toUint() = this.toInt() and 0xff

fun String.trigram() : Int {
    val bytes = this.toByteArray()
    return (bytes[0].toUint() shl 16) or (bytes[1].toUint() shl 8) or bytes[2].toUint()
}

// An Index implements read-only access to a trigram index.
class Index(val data: ByteBuffer, offsets : Int) {
    val pathData: Int = data.getInt(offsets)
    val nameData: Int = data.getInt(offsets + 4)
    val postData: Int = data.getInt(offsets + 8)
    val nameIndex: Int = data.getInt(offsets + 12)
    val postIndex: Int = data.getInt(offsets + 16)
    val numName: Int = (postIndex - nameIndex) / 4 - 1
    val numPost: Int = (offsets - postIndex) / Const.POST_ENTRY_SIZE

    companion object {
        fun open(file : File) : Index {
            val len = file.length()
            val data =
                    FileInputStream(file).channel.map(FileChannel.MapMode.READ_ONLY, 0L, len)
            val offsets = len - Const.TRAILER_MAGIC.length - 5 * 4;
            if (offsets < 0 || offsets >= Int.MAX_VALUE) {
                throw IllegalArgumentException(file.toString())
            }

            return Index(data, offsets.toInt())
        }
    }

    fun readInt32(offset : Int) : Int = data.getInt(offset)

    fun readString(offset: Int) : String =
        buildString {
            var i = 0
            while (data[offset + i] != 0.toByte()) {
                append(data[offset + i].toChar())
                i++
            }
        }

    fun readPaths(): List<String> {
        var off = pathData
        val paths = mutableListOf<String>()
        while(true) {
            val s = readString(off)
            if (s.isEmpty()) break;
            paths.add(s)
            off += s.length + 1
        }
        return paths
    }

    fun readName(fieldId: Int) : String {
        var off = readInt32(nameIndex + 4 * fieldId)
        return readString(nameData + off)
    }

    fun readListAt(off: Int) {
        // TODO:
    }

    fun readTrigram(off : Int) : Int =
            (data[off].toUint() shl 16) or (data[off + 1].toUint() shl 8) or (data[off + 2].toUint())

    fun createPostReader(trigram: Int) : PostReader? {
        val ix = getPostIndex(trigram) ?: return null
        val off = postIndex + ix * Const.POST_ENTRY_SIZE
        val count = readInt32(off + 3)
        val offset = postData + readInt32(off + 7) + 3
        // println(String.format("%06x: %d at %d (%x)", readTrigram(off), count, offset, offset))
        if (readTrigram(off) != trigram) return null
        return PostReader(offset, count)
    }

    private fun getPostIndex(trigram: Int): Int? {
        // TODO: use binary search
        return (0 until numPost).firstOrNull {
            readTrigram(postIndex + it * Const.POST_ENTRY_SIZE) == trigram
        }
    }

    inner class PostReader(var offset: Int, var count: Int) {
        var fileId :Int = -1

        fun max() = count

        fun next() : Boolean {
            count--
            val delta = readVarInt()
            if (delta == 0) {
                fileId = -1
                return false
            }
            fileId += delta
            return true
        }

        fun readVarInt() : Int {
            var res : Int = 0
            var s = 0
            do {
                val b = data.get(offset).toInt()
                res = res or ((b and 0x7f) shl s)
                offset++
                s+=7
            } while ((b and 0x80) != 0)
            return res
        }

    }

    fun postingList(trigram: Int): Collection<Int> {
        val r = createPostReader(trigram)
        val res = mutableSetOf<Int>()
        if (r != null) {
            while (r.next()) {
                val fileId = r.fileId
                res.add(fileId)
            }
        }
        return res
    }

    // postingAnd and postingOr can be replaced with set operations

    fun postingQuery(q : Query) : Collection<Int> {
        val res = mutableSetOf<Int>()
        when (q.Op) {
            QueryOp.NONE -> return emptySet()
            QueryOp.ALL -> {
                res += 0 until numName
            }
            QueryOp.AND -> {
                var first = true
                for (s in q.Trigram) {
                    if (first) {
                        res += postingList(s.trigram())
                        first = false
                    } else {
                        res.retainAll(postingList(s.trigram()))
                    }
                    if (res.isEmpty()) {
                        return emptySet()
                    }
                }
                for (s in q.Sub) {
                    res.retainAll(postingQuery(s))
                    if (res.isEmpty()) {
                        return emptySet()
                    }
                }
            }
            QueryOp.OR -> {
                for (s in q.Trigram) {
                    res += postingList(s.trigram())
                }
                for (s in q.Sub) {
                    res += postingQuery(s)
                }
            }
        }
        return res
    }

    fun dumpPosting() {
        for (i in 0 until numPost) {
            val j = postIndex + i * Const.POST_ENTRY_SIZE
            val t = readTrigram(j)
            val count = readInt32(j + 3)
            val offset = readInt32(j + 7)
            println(String.format("%06x: %d at %d (%x)", t, count, offset, offset))
        }
    }
}