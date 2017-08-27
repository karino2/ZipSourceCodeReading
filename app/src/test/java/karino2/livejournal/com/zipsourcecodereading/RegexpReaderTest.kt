package karino2.livejournal.com.zipsourcecodereading

import com.google.re2j.Pattern
import org.junit.Test

import org.junit.Assert.*
import java.io.ByteArrayInputStream

/**
 * Created by _ on 2017/08/27.
 */
public class RegexpReaderTest {


    @Test public fun readBasic() {
        val content = """
First line
abc def
xyz ab de
fxy
"""

        val patText = "(?m)abc"


        val matches = executeRead(content, patText)

        assertEquals(1, matches.size)
        val mat = matches.get(0)

        assertEquals("fname", mat.fentry)
        assertEquals("abc def", mat.line)
        assertEquals(3, mat.lineNumber)
    }

    @Test public fun read_matchLastLine() {
        val content = """First line
fxy"""

        val patText = "(?m)fxy"


        val matches = executeRead(content, patText)

        assertEquals(1, matches.size)
        val mat = matches.get(0)

        assertEquals("fxy", mat.line)
        assertEquals(2, mat.lineNumber)
    }

    @Test public fun read_matchTwice() {
        val content = """
First line
abc def
xyz ab de
fxy
"""

        val patText = "(?m)ab"


        val matches = executeRead(content, patText)

        assertEquals(2, matches.size)
    }

    private fun executeRead(content: String, patText: String): ArrayList<RegexpReader.MatchEntry> {
        val inp = ByteArrayInputStream(content.toByteArray())
        val pat = Pattern.compile(patText)
        val reader = RegexpReader(pat)

        val matches = arrayListOf<RegexpReader.MatchEntry>()
        reader.Read(inp, "fname", 1)
                .subscribe { mat ->
                    matches.add(mat)
                }
        return matches
    }
}