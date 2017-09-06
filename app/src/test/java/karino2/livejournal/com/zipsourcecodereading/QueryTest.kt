package karino2.livejournal.com.zipsourcecodereading

import com.google.re2j.Parser
import com.google.re2j.RE2
import org.junit.Test
import org.junit.Assert.*

class QueryTest {

    @Test
    fun singleTest() {
        val expect = "\"Abc\" \"bcd\" \"cde\" \"def\""
        val re = Parser.parse("Abcdef", RE2.PERL)
        val q = Query.fromRegexp(re)
        assertEquals(expect, q.toString())
    }


    //  port of codesearch/index/regexp_test.go.
    data class TE(val re: String, val query: String)

    // for investigation
    @Test
    fun singleTETest() {
        verifyOneTE(0,
                TE("(?i)a~~","(\"A~~\"|\"a~~\")")
//                TE("(a|ab)cde","\"cde\" (\"abc\" \"bcd\")|(\"acd\")")
                // TE("(abc)(def)", "\"abc\" \"bcd\" \"cde\" \"def\"")
                // TE("abc.*(def|ghi)", "\"abc\" (\"def\"|\"ghi\")")
                )
    }

    val normalEntries = arrayOf(
            TE("Abcdef", "\"Abc\" \"bcd\" \"cde\" \"def\""),
            TE("(abc)(def)", "\"abc\" \"bcd\" \"cde\" \"def\""),
            TE("abc.*(def|ghi)", "\"abc\" (\"def\"|\"ghi\")"),
            TE("abc(def|ghi)","\"abc\" (\"bcd\" \"cde\" \"def\")|(\"bcg\" \"cgh\" \"ghi\")"),
            TE("a+hello","\"ahe\" \"ell\" \"hel\" \"llo\""),
            TE("(a+hello|b+world)","(\"ahe\" \"ell\" \"hel\" \"llo\")|(\"bwo\" \"orl\" \"rld\" \"wor\")"),
            TE("a*bbb","\"bbb\""),
            TE("a?bbb","\"bbb\""),
            TE("(bbb)a?","\"bbb\""),
            TE("(bbb)a*","\"bbb\""),
            TE("^abc","\"abc\""),
            TE("abc\$","\"abc\""),
            TE("ab[cde]f","(\"abc\" \"bcf\")|(\"abd\" \"bdf\")|(\"abe\" \"bef\")"),
            TE("(abc|bac)de","\"cde\" (\"abc\" \"bcd\")|(\"acd\" \"bac\")"),
            // These don't have enough letters for a trigram, so they return the
            // always matching query "+".
            TE("ab[^cde]f","+"),
            TE("ab.f","+"),
            TE(".","+"),
            TE("()","+"),

            // No matches.
            TE("[^\\s\\S]","-")
    )
    @Test
    fun normalTests() {
        verifyTestEntries(normalEntries)
    }

    val factoringEntries = arrayOf(

            // Factoring works.
            TE("(abc|abc)","\"abc\""),
            TE("(ab|ab)c","\"abc\""),
            TE("ab(cab|cat)","\"abc\" \"bca\" (\"cab\"|\"cat\")"),
            TE("(z*(abc|def)z*)(z*(abc|def)z*)","(\"abc\"|\"def\")"),
            TE("(z*abcz*defz*)|(z*abcz*defz*)","\"abc\" \"def\""),
            TE("(z*abcz*defz*(ghi|jkl)z*)|(z*abcz*defz*(mno|prs)z*)","\"abc\" \"def\" (\"ghi\"|\"jkl\"|\"mno\"|\"prs\")"),
            TE("(z*(abcz*def)|(ghiz*jkl)z*)|(z*(mnoz*prs)|(tuvz*wxy)z*)","(\"abc\" \"def\")|(\"ghi\" \"jkl\")|(\"mno\" \"prs\")|(\"tuv\" \"wxy\")"),
            TE("(z*abcz*defz*)(z*(ghi|jkl)z*)","\"abc\" \"def\" (\"ghi\"|\"jkl\")"),
            TE("(z*abcz*defz*)|(z*(ghi|jkl)z*)","(\"ghi\"|\"jkl\")|(\"abc\" \"def\")"))
    @Test
    fun factoringTests() {
        verifyTestEntries(factoringEntries)
    }
    val multipleEntries = arrayOf(
            // analyze keeps track of multiple possible prefix/suffixes.
            TE("[ab][cd][ef]","(\"ace\"|\"acf\"|\"ade\"|\"adf\"|\"bce\"|\"bcf\"|\"bde\"|\"bdf\")"),
            TE("ab[cd]e","(\"abc\" \"bce\")|(\"abd\" \"bde\")")
    )
    @Test
    fun multiplePrefixSuffixTests() {
        verifyTestEntries(multipleEntries)
    }

    val diffsuffEntries = arrayOf(
            // Different sized suffixes.
            TE("(a|ab)cde","\"cde\" (\"abc\" \"bcd\")|(\"acd\")"), // This line is number 30
            TE("(a|b|c|d)(ef|g|hi|j)","+"),

            TE("(?s).","+"))

    @Test
    fun differentSizedSuffixTests() {
        verifyTestEntries(diffsuffEntries)
    }

    val expandEntries = arrayOf(
            // Expanding case.
            TE("(?i)a~~","(\"A~~\"|\"a~~\")"),
            TE("(?i)ab~","(\"AB~\"|\"Ab~\"|\"aB~\"|\"ab~\")"),
            TE("(?i)abc","(\"ABC\"|\"ABc\"|\"AbC\"|\"Abc\"|\"aBC\"|\"aBc\"|\"abC\"|\"abc\")"),
            TE("(?i)abc|def","(\"ABC\"|\"ABc\"|\"AbC\"|\"Abc\"|\"DEF\"|\"DEf\"|\"DeF\"|\"Def\"|\"aBC\"|\"aBc\"|\"abC\"|\"abc\"|\"dEF\"|\"dEf\"|\"deF\"|\"def\")"),
            TE("(?i)abcd","(\"ABC\"|\"ABc\"|\"AbC\"|\"Abc\"|\"aBC\"|\"aBc\"|\"abC\"|\"abc\") (\"BCD\"|\"BCd\"|\"BcD\"|\"Bcd\"|\"bCD\"|\"bCd\"|\"bcD\"|\"bcd\")"),
            TE("(?i)abc|abc","(\"ABC\"|\"ABc\"|\"AbC\"|\"Abc\"|\"aBC\"|\"aBc\"|\"abC\"|\"abc\")")
    )
    @Test
    fun expandEntriesTests() {
        verifyTestEntries(expandEntries)
    }
    // Word boundary.
    val wordBoundaryEntries = arrayOf(
            TE("\\b","+"),
            TE("\\B","+"),
            TE("\\babc","\"abc\""),
            TE("\\Babc","\"abc\""),
            TE("abc\\b","\"abc\""),
            TE("abc\\B","\"abc\""),
            TE("ab\\bc","\"abc\""),
            TE("ab\\Bc","\"abc\"")
    )
    @Test
    fun wordBoundaryTests() {
        verifyTestEntries(wordBoundaryEntries)
    }

    private fun verifyTestEntries(entries: Array<TE>) {
        for ((i, te) in entries.withIndex()) {
            verifyOneTE(i, te)
        }
    }

    private fun verifyOneTE(i: Int, te: TE) {
        val re = Parser.parse(te.re, RE2.PERL)
        val q = Query.fromRegexp(re)
        assertEquals("$i test: ${te.re} -> ${te.query}", te.query, q.toString())
    }
}