package com.livejournal.karino2.zipsourcecodereading

import com.google.re2j.RE2
import com.google.re2j.Regexp
import com.google.re2j.Unicode
import com.google.re2j.Utils

fun Set<String>.cross(other : Set<String>, isSuffix: Boolean) : MutableSet<String> {
    val ret = mutableSetOf<String>()
    for(ss in this) {
        for (tt in other) {
            ret.add(ss+tt)
        }
    }
    if(isSuffix) {
        return ret.cleanAsSuffix()
    }
    return ret.cleanAsPrefix()
}

// in my understanding, cleanAsPrefix() and cleanAsSuffix() need to do nothing.
// But sort here to keep the result of test contents the same as golang version.
fun Set<String>.cleanAsPrefix() : MutableSet<String> {
    // return this.toMutableSet()
    return this.toSortedSet().toMutableSet()
}
fun Set<String>.dropUselessPrefix() : MutableSet<String> {
    val ret = mutableListOf<String>()
    var lastElem : String? = null
    for(item in this.toSortedSet()) {
        val already = lastElem?.let { item.startsWith(lastElem!!) and ((lastElem != "") and (item != ""))} ?: false
        if(!already) {
            ret.add(item)
            lastElem = item
        }
    }
    return ret.toMutableSet()
}

fun Set<String>.cleanAsSuffix() : MutableSet<String> {
//     return this.toMutableSet()
    return toSortedSet(object : Comparator<String>{
        override fun compare(a :String, b: String) : Int {
            for(i in 1..(minOf(a.length, b.length))) {
                val ai = a[a.length-i]
                val bi = b[b.length-i]
                if(ai != bi)
                    return ai-bi
            }
            return a.length-b.length
        }
    }).toMutableSet()
}
fun Set<String>.dropUselessSuffix() : MutableSet<String> {
    val ret = mutableListOf<String>()
    var lastElem : String? = null

    // "cab", "cabc", "abc", "dea", "def", "bc", "dab", "dbc", "ab", "aab" -> [dea, ab, aab, cab, dab, bc, abc, cabc, dbc, def]
    val sorted = this.toSortedSet(object : Comparator<String>{
        override fun compare(a :String, b: String) : Int {
            for(i in 1..(minOf(a.length, b.length))) {
                val ai = a[a.length-i]
                val bi = b[b.length-i]
                if(ai != bi)
                    return ai-bi
            }
            return a.length-b.length
        }
    })
    for(item in sorted) {
        val already = lastElem?.let { item.endsWith(lastElem!!) } ?: false
        if(!already) {
            ret.add(item)
            lastElem = item
        }
    }
    return ret.toMutableSet()
}


fun Set<String>.minLen() : Int  = this.map { it.length }.min() ?: 0


data class RegexpInfo(
        // canEmpty records whether the regexp matches the empty string
        var canEmpty: Boolean =false,
        // exact is the exact set of strings matching the regexp.
        var exact : MutableSet<String> = mutableSetOf<String>(),
        // if exact is nil, prefix is the set of possible match prefixes,
        // and suffix is the set of possible match suffixes.
        var prefix : MutableSet<String> = mutableSetOf<String>(), // otherwise: the exact set of matching prefixes ...
        var suffix : MutableSet<String> = mutableSetOf<String>(), // ... and suffixes
        // match records a query that must be satisfied by any
        // match for the regexp, in addition to the information
        // recorded above.
        var match: Query = Query.noneQuery
        ) {

    companion object {
        fun noMatch() = RegexpInfo(match=Query.noneQuery)
        fun emptyString() = RegexpInfo(canEmpty = true, exact = mutableSetOf(""), match = Query.allQuery)
        fun anyChar() = RegexpInfo(match=Query.allQuery, prefix=mutableSetOf(""), suffix=mutableSetOf(""))
        fun anyMatch() = RegexpInfo(canEmpty = true, prefix = mutableSetOf(""), suffix = mutableSetOf(""), match = Query.allQuery)

        val MAX_SET=20
        val MAX_EXACT=7



        // fold is the usual higher-order function.
        fun fold(f: (RegexpInfo, RegexpInfo)->RegexpInfo, sub: Array<Regexp>, zero: RegexpInfo) : RegexpInfo {
            if(sub.size == 0) {
                return zero
            }
            if(sub.size == 1) {
                return analyze(sub[0])
            }
            var info = f(analyze(sub[0]), analyze(sub[1]))
            for(i in (2 until sub.size)) {
                info = f(info, analyze(sub[i]))
            }
            return info
        }

        // concat returns the regexp info for xy given x and y.
        fun concat(x: RegexpInfo, y: RegexpInfo) : RegexpInfo {
            var xy = RegexpInfo(match= x.match.and(y.match))
            if(x.exact.isNotEmpty() and y.exact.isNotEmpty()) {
                xy.exact = x.exact.cross(y.exact, false)
            }else {
                if(x.exact.isNotEmpty()) {
                    xy.prefix = x.exact.cross(y.prefix, false)
                }else {
                    xy.prefix = x.prefix
                    if(x.canEmpty) {
                        xy.prefix.addAll(y.prefix)
                        xy.prefix = xy.prefix.cleanAsPrefix()
                    }
                }
                if(y.exact.isNotEmpty()) {
                    xy.suffix = x.suffix.cross(y.exact, true)
                } else {
                    xy.suffix = y.suffix
                    if(y.canEmpty) {
                        xy.suffix.addAll(x.suffix)
                        xy.suffix = xy.suffix.cleanAsSuffix()
                    }
                }

            }
            // If all the possible strings in the cross product of x.suffix
            // and y.prefix are long enough, then the trigram for one
            // of them must be present and would not necessarily be
            // accounted for in xy.prefix or xy.suffix yet.  Cut things off
            // at maxSet just to keep the sets manageable.
            if( x.exact.isEmpty() and y.exact.isEmpty() and
                    (y.suffix.size <= MAX_SET) and (y.prefix.size <= MAX_SET) and
                    (x.suffix.minLen() + y.prefix.minLen() >= 3)) {
                xy.match = xy.match.andTrigrams(x.suffix.cross(y.prefix, false))
            }

            xy.simplify(false)
            return xy
        }

        // alternate returns the regexpInfo for x|y given x and y.
        fun alternate(x: RegexpInfo, y:RegexpInfo) : RegexpInfo {
            var xy = RegexpInfo()
            if(x.exact.isNotEmpty() and y.exact.isNotEmpty()) {
                xy.exact = x.exact.union(y.exact).cleanAsPrefix()
            } else if(x.exact.isNotEmpty()) {
                xy.prefix = x.exact.union(y.prefix).cleanAsPrefix()
                xy.suffix = x.exact.union(y.suffix).cleanAsSuffix()
                x.addExact()
            } else if(y.exact.isNotEmpty()) {
                xy.prefix = x.prefix.union(y.exact).cleanAsPrefix()
                xy.suffix = x.suffix.union(y.exact).cleanAsSuffix()
                y.addExact()
            } else {
                xy.prefix = x.prefix.union(y.prefix).cleanAsPrefix()
                xy.suffix = x.suffix.union(y.suffix).cleanAsSuffix()
            }
            xy.canEmpty = x.canEmpty or y.canEmpty
            xy.match = x.match.or(y.match)
            xy.simplify(false)
            return xy
        }

        fun analyze(re : Regexp) : RegexpInfo {
            when(re.op) {
                Regexp.Op.NO_MATCH -> return noMatch()
                Regexp.Op.EMPTY_MATCH, Regexp.Op.BEGIN_LINE,
                Regexp.Op.END_LINE, Regexp.Op.BEGIN_TEXT, Regexp.Op.END_TEXT,
                Regexp.Op.WORD_BOUNDARY, Regexp.Op.NO_WORD_BOUNDARY -> return emptyString()
                Regexp.Op.LITERAL -> {
                    if((re.flags and RE2.FOLD_CASE) != 0) {
                        when(re.runes.size) {
                            0 -> return emptyString()
                            1 -> {
                                // Single-letter case-folded string:
                                // rewrite into char class and analyze.
                                var re1 = Regexp(Regexp.Op.CHAR_CLASS)
                                val r0 = re.runes[0]
                                var r1 = Unicode.simpleFold(r0)
                                val runes = ArrayList<Int>()
                                runes.add(r0)
                                runes.add(r0)
                                while(r1 != r0) {
                                    runes.add(r1)
                                    runes.add(r1)
                                    r1 = Unicode.simpleFold(r1)
                                }
                                re1.runes = runes.toIntArray()
                                return analyze(re1)
                            }
                            else -> {
                                // Multi-letter case-folded string:
                                // treat as concatenation of single-letter case-folded strings.
                                val re1 = Regexp(Regexp.Op.LITERAL)
                                re1.flags = RE2.FOLD_CASE
                                var info = emptyString()
                                for(i in re.runes.indices) {
                                    re1.runes = re.runes.slice(i until (i+1)).toIntArray()
                                    info = concat(info, analyze(re1))
                                }
                                return info
                            }
                        }

                    } else {
                        val info = RegexpInfo(exact = mutableSetOf<String>(re.runes.map { Utils.runeToString(it) }.joinToString("")))
                        info.match = Query.allQuery
                        info.simplify(false)
                        return info
                    }
                }
                Regexp.Op.ANY_CHAR_NOT_NL, Regexp.Op.ANY_CHAR ->
                        return anyChar()
                Regexp.Op.CAPTURE -> return analyze(re.subs[0])
                Regexp.Op.CONCAT -> return fold(RegexpInfo.Companion::concat, re.subs, emptyString())
                Regexp.Op.ALTERNATE -> return fold(Companion::alternate, re.subs, noMatch())
                Regexp.Op.QUEST -> return alternate(analyze(re.subs[0]), emptyString())

                // We don't know anything, so assume the worst.
                Regexp.Op.STAR -> return anyMatch()

                Regexp.Op.REPEAT, Regexp.Op.PLUS -> {
                    if((re.op == Regexp.Op.REPEAT) and (re.min == 0)) {
                        // Like STAR
                        return anyMatch()
                    }
                    // x+
                    // Since there has to be at least one x, the prefixes and suffixes
                    // stay the same.  If x was exact, it isn't anymore.
                    val info = analyze(re.subs[0])
                    if(info.exact.isNotEmpty()) {
                        info.prefix = info.exact
                        info.suffix = info.exact.toMutableSet()
                        info.exact = mutableSetOf<String>()
                    }
                    return info
                }
                Regexp.Op.CHAR_CLASS -> {
                    if(re.runes.size == 0) {
                        return noMatch()
                    }

                    val info = RegexpInfo(match= Query.allQuery)
                    if(re.runes.size == 1) {
                        info.exact = mutableSetOf<String>(Utils.runeToString(re.runes[0]))
                        info.simplify(false)
                        return info
                    }
                    var n = 0
                    for(i in (0 until re.runes.size step 2)) {
                        n+= re.runes[i+1]-re.runes[i]
                    }
                    // If the class is too large, it's okay to overestimate.
                    if(n > 100) {
                        return anyChar()
                    }
                    info.exact = mutableSetOf<String>()
                    for(i in (0 until re.runes.size step 2)) {
                        val lo = re.runes[i]
                        val hi = re.runes[i+1]
                        for(rr in (lo..hi)) {
                            info.exact.add((rr.toChar().toString()))
                        }
                    }
                    info.simplify(false)
                    return info
                }
            }
            throw RuntimeException("Never reached here.")
        }
    }

    fun addExact() {
        if(exact.isNotEmpty()) {
            match = match.andTrigrams(exact)
        }
    }


    fun simplify(force: Boolean) {
        // If there are now too many exact strings,
        // loop over them, adding trigrams and moving
        // the relevant pieces into prefix and suffix.
        if((this.exact.size > MAX_EXACT) or ((this.exact.minLen() >= 3) and force) or
                (this.exact.minLen() >= 4)) {
            addExact()
            for(s in exact) {
                val n = s.length
                if(n < 3) {
                    prefix.add(s)
                    suffix.add(s)
                } else {
                    prefix.add(s.slice(0 until 2))
                    suffix.add(s.slice((n-2) until n))
                }
            }
            exact.clear()
        }

        if(exact.isEmpty()) {
            simplifyPrefixSet()
            simplifySuffixSet()
        }
    }

    // part of simplifySet. maybe bad naming
    fun splitPrefixToTrigram() : MutableSet<String> {
        var res = prefix.toMutableSet()
        for(triLen in (3 downTo 1)) {
            if((triLen != 3) and (res.size < MAX_SET))
                break
            // Replace set by strings of length n-1.
            prefix.filter{ it.length >= triLen}.forEach{ res.add(it.slice(0 until (triLen-1))) }
            res = res.cleanAsPrefix()
        }
        res = res.dropUselessPrefix()
        return res
    }

    // part of simplifySet. maybe bad naming
    fun splitSuffixToTrigram() : MutableSet<String> {
        var res = suffix.toMutableSet()
        for(triLen in (3 downTo 1)) {
            if((triLen != 3) and (res.size < MAX_SET))
                break
            // Replace set by strings of length n-1.
            // 					str = str[len(str)-n+1:]
            suffix.filter{it.length >= triLen}.forEach{ res.add(it.slice(it.length-(triLen-1) until it.length)) }
            res = res.cleanAsSuffix()
        }
        res = res.dropUselessSuffix()
        return res

    }

    fun simplifyPrefixSet() {
        prefix = prefix.cleanAsPrefix()
        match = match.andTrigrams(prefix)
        prefix = splitPrefixToTrigram()
    }

    fun simplifySuffixSet() {
        suffix = suffix.cleanAsSuffix()
        match = match.andTrigrams(suffix)
        suffix = splitSuffixToTrigram()
    }

}