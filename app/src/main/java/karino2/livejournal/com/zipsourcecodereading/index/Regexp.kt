package karino2.livejournal.com.zipsourcecodereading.index

sealed class Query()

class QAll : Query()
class QNone : Query()
class QAnd(val trigrams : Collection<String>, val sub: Collection<Query>) : Query()
class QOr(val trigrams : Collection<String>, val sub: Collection<Query>) : Query()

