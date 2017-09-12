package karino2.livejournal.com.zipsourcecodereading

enum class QueryOp {
    ALL, // Everything matches
    NONE, // Nothing matches
    AND, // All in Sub and Trigram must match
    OR  // At least one in Sub or Trigram must match
}