package karino2.livejournal.com.zipsourcecodereading

import com.google.re2j.Parser
import com.google.re2j.RE2
import karino2.livejournal.com.zipsourcecodereading.index.Index
import karino2.livejournal.com.zipsourcecodereading.index.trigram
import org.junit.Test
import java.io.File
import org.junit.Assert.*

/**
 * Created by _ on 2017/09/12.
 */
class IndexTest {
    @Test
    fun testPostingList() {

        val index = openTestIndex()


        val actual = index.postingList(" Ed".trigram())

        val expects = arrayOf("abc_search_view.xml",
                "BookActivity.java",
                "BookListActivity.java",
                "CellListAdapter.java",
                "EditActivity.java")

        assertEquals(5, actual.size)
        for ((i, fileNo) in actual.withIndex()) {
            assertTrue(index.readName(fileNo).endsWith(expects[i]))
        }

    }

    @Test
    fun testPostingQuery() {
        val index = openTestIndex()
        val q = Query.fromRegexp(Parser.parse("class EditActivity", RE2.PERL))
        // println(q.toString())

        val actual = index.postingQuery(q)
        assertEquals(1, actual.size)

        assertTrue(index.readName(actual.first()).endsWith("EditActivity.java"))
    }

    private fun openTestIndex(): Index {
        val file = File(javaClass.classLoader.getResource("MeatPieDay.idx").path)
        val index = Index.open(file)
        return index
    }
}