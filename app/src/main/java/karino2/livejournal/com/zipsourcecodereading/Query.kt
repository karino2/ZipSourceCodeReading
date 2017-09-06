package karino2.livejournal.com.zipsourcecodereading

import com.google.re2j.Regexp

/**
 * Created by _ on 2017/09/04.
 */


/*
This class is port of Query at codesearch/index/regexp.go.
Query is trigram-ized pattern that contain larger than or equal to original regexp pattern.
 */
class Query(var Op : Query.QueryOp = Query.QueryOp.NONE,
             var Trigram: MutableSet<String> = mutableSetOf<String>(),
            var Sub: ArrayList<Query> = arrayListOf<Query>()) {
    companion object {
        fun fromRegexp(re: Regexp) : Query {
            val info = RegexpInfo.analyze(re)
            info.simplify(true)
            info.addExact()
            return info.match
        }

        val allQuery = Query(Op = QueryOp.ALL)
        val noneQuery = Query(Op = QueryOp.NONE)


        fun trigramsImply(trigrams: Set<String>, q: Query): Boolean {
            when(q.Op) {
                QueryOp.OR-> {
                    if(q.Sub.any { trigramsImply(trigrams, it) }) {
                        return true
                    }
                    if(trigrams.any{
                        setOf<String>(it).isSubsetOf(q.Trigram)
                    }) {
                        return true
                    }
                    return false
                }
                QueryOp.AND -> {
                    if(q.Sub.any{ !trigramsImply(trigrams, it)}) {
                        return false
                    }
                    if(!q.Trigram.isSubsetOf(trigrams)) {
                        return false
                    }
                    return true
                }
                else -> return false
            }
        }

        // isSubsetOf returns true if all strings in "this" are also in other
        // karino: may be this implementation is wrong because we don't care inclusion relation for each item, but original code also seems not.
        fun Set<String>.isSubsetOf(other: Set<String>): Boolean {
            return this.all { it in other }
        }

    }

    enum class QueryOp {
        ALL, // Everything matches
        NONE, // Nothing matches
        AND, // All in Sub and Trigram must match
        OR  // At least one in Sub or Trigram must match
    }

    // andOr returns the query q AND r or q OR r, possibly reusing q's and r's storage.
    // It works hard to avoid creating unnecessarily complicated structures.
    fun andOr(r_in:Query, op: QueryOp) : Query {
        var r = r_in
        var q = this

        val opstr = if(op == QueryOp.AND) "&" else "|"
        if((q.Trigram.size == 0) and (q.Sub.size == 1) ) {
            q = q.Sub[0]
        }
        if ((r.Trigram.size == 0) and (r.Sub.size == 1)) {
            r = r.Sub[0]
        }

        // Boolean simplification.
        // If q ⇒ r, q AND r ≡ q.
        // If q ⇒ r, q OR r ≡ r.
        if(q.implies(r)) {
            if(op == QueryOp.AND) {
                return q
            } else {
                return r
            }
        }
        if(r.implies(q)) {
            if(op == QueryOp.AND) {
                return r
            } else {
                return q
            }
        }

        // Both q and r are QAnd or QOr.
        // If they match or can be made to match, merge.
        val qAtom = (q.Trigram.size == 1) and (q.Sub.size == 0)
        val rAtom = (r.Trigram.size == 1) and (q.Sub.size == 0)
        if(q.Op == op && ((r.Op == op) or rAtom)) {
            q.Trigram = q.Trigram.union(r.Trigram).cleanAsPrefix()
            q.Sub.addAll(r.Sub)
            return q
        }
        if((r.Op == op) and qAtom) {
            r.Trigram = r.Trigram.union(q.Trigram).cleanAsPrefix()
            return r
        }
        if(qAtom and rAtom) {
            q.Op = op
            q.Trigram = q.Trigram.union(r.Trigram).cleanAsPrefix()
            return q
        }

        // If one matches the op, add the other to it.
        if(q.Op == op) {
            q.Sub.add(r)
            return q
        }
        if(r.Op == op) {
            r.Sub.add(q)
            return r
        }
        // We are creating an AND of ORs or an OR of ANDs.
        // Factor out common trigrams, if any.
        val common = mutableSetOf<String>()
        var i = 0
        var j = 0
        var wi = 0
        var wj = 0
        val qtrigs = q.Trigram.toSortedSet().toMutableList()
        val rtrigs = r.Trigram.toSortedSet().toMutableList()
        val newQtrigs = arrayListOf<String>()
        val newRTrigs = arrayListOf<String>()
        while((i < qtrigs.size) and (j < rtrigs.size)) {
            val qt = qtrigs[i]
            val rt = rtrigs[j]
            if(qt < rt) {
                // newQtrigs[wi] = qt
                // wi++
                newQtrigs.add(qt)
                i++
            } else if(qt > rt) {
                // newRTrigs[wj] = rt
                // wj++
                newRTrigs.add(rt)
                j++
            } else {
                common.add(qt)
                i++
                j++
            }
        }
        newQtrigs.addAll(qtrigs.slice(i until qtrigs.size))
        newRTrigs.addAll(rtrigs.slice(j until rtrigs.size))
        q.Trigram = newQtrigs.toMutableSet()
        r.Trigram = newRTrigs.toMutableSet()

        if(common.size > 0) {
            // If there were common trigrams, rewrite
            //
            //	(abc|def|ghi|jkl) AND (abc|def|mno|prs) =>
            //		(abc|def) OR ((ghi|jkl) AND (mno|prs))
            //
            //	(abc&def&ghi&jkl) OR (abc&def&mno&prs) =>
            //		(abc&def) AND ((ghi&jkl) OR (mno&prs))
            //
            // Build up the right one of
            //	(ghi|jkl) AND (mno|prs)
            //	(ghi&jkl) OR (mno&prs)
            // Call andOr recursively in case q and r can now be simplified
            // (we removed some trigrams).
            val s = q.andOr(r, op)
            val otherOp = if(op == QueryOp.OR) QueryOp.AND else QueryOp.OR
            val t = Query(Op=otherOp, Trigram=common)
            return t.andOr(s, t.Op)
        }
        // ...
        // Otherwise just create the op.
        return Query(Op=op, Sub = arrayListOf(q, r))
    }

    // implies reports whether q implies r.
    // It is okay for it to return false negatives.
    private fun implies(r: Query): Boolean {
        if((this.Op == QueryOp.NONE) or (r.Op ==QueryOp.ALL)) {
            // False implies everything.
            // Everything implies True.
            return true
        }
        if((this.Op == QueryOp.ALL) or (r.Op == QueryOp.NONE)) {
            // True implies nothing.
            // Nothing implies False.
            return false
        }
        if((this.Op == QueryOp.AND) or
                ((this.Op == QueryOp.OR) and (this.Trigram.size == 1) and (this.Sub.size == 0))) {
            return trigramsImply(this.Trigram, r)
        }

        if((this.Op == QueryOp.OR) and (r.Op == QueryOp.OR) and
                (this.Trigram.size > 0) and (this.Sub.size == 0) and
                this.Trigram.isSubsetOf(r.Trigram)) {
            return true
        }
        return false
    }



    fun and(r: Query) : Query {
        return andOr(r, QueryOp.AND)
    }

    fun or(r: Query): Query {
        return andOr(r, QueryOp.OR)
    }

    // andTrigrams returns q AND the OR of the AND of the trigrams present in each string.
    fun andTrigrams(t: Set<String>) : Query {
        if(t.minLen() < 3) {
            // If there is a short string, we can't guarantee
            // that any trigrams must be present, so use ALL.
            // q AND ALL = q.
            return this
        }
        var orQuery = noneQuery
        for(tt in t) {
            val trig = mutableSetOf<String>()
            for(i in 0..(tt.length-3)) {
                trig.add(tt.slice(i until (i+3)))
            }
            orQuery = orQuery.or(Query(Op = QueryOp.AND, Trigram = trig.cleanAsPrefix()))
        }
        return this.and(orQuery)
    }

    override fun toString() : String {
        if(Op == QueryOp.NONE) {
            return "-"
        }
        if(Op == QueryOp.ALL) {
            return "+"
        }
        if((Sub.size == 0) and (Trigram.size == 1)) {
            return "\"${Trigram.first()}\""
        }
        var s = StringBuilder("")
        var sjoin = ""
        var end = ""
        var tjoin = ""
        if(Op == QueryOp.AND) {
            sjoin = " "
            tjoin = " "
        } else {
            s.append("(")
            sjoin = ")|("
            end = ")"
            tjoin = "|"
        }
        for((i, t) in Trigram.withIndex()) {
            if(i > 0) {
                s.append(tjoin)
            }
            s.append("\"$t\"")
        }
        if(Sub.size > 0) {
            if(Trigram.size > 0) {
                s.append(sjoin)
            }
            s.append(Sub.first().toString())
            for(i in 1 until Sub.size) {
                s.append(sjoin)
                s.append(Sub[i].toString())
            }
        }
        s.append(end)
        return s.toString()
    }

}