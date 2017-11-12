package com.livejournal.karino2.zipsourcecodereading.index

import java.io.File

fun main(args:Array<String>) {
    var index = Index.open(File(args[0]))

    for (fileNo in index.postingList(args[1].trigram())) {
        println(index.readName(fileNo))
    }
}