package com.example.camerademo.lrc

class LrcRow(row: String) {

    var startTime: Int = 0
    var words: MutableList<LrcWord> = mutableListOf()

    init {
        startTime = stringToMillisecond(row.substring(row.indexOf("[") + 1, row.indexOf("]")))
        var wordStart = row.indexOf("]")
        while (true) {
            val wordEnd = row.indexOf("<", wordStart)
            val timeStart = row.indexOf("<", wordStart)
            val timeEnd = row.indexOf(">", wordStart)
            words.add(
                LrcWord(
                    row.substring(wordStart + 1, wordEnd),
                    stringToMillisecond(row.substring(timeStart + 1, timeEnd)),
                    row.substring(timeStart + 1, timeEnd)
                )
            )
            if (row.indexOf(">", timeEnd + 1) != -1) {
                wordStart = timeEnd + 1
            } else {
                break
            }
        }
    }

    private fun stringToMillisecond(timeAsString: String): Int {

        var total = 0

        val min = timeAsString.substring(0, timeAsString.indexOf(":")).toInt()
        total += min * 60 * 1000

        val seconds = timeAsString.substring(
                timeAsString.indexOf(":") + 1, timeAsString.indexOf(".")
        ).toInt()
        total += seconds * 1000

        val centiSecondsIndex = timeAsString.indexOf(".")
        val centiSeconds = timeAsString.substring(centiSecondsIndex + 1).toInt()
        total += centiSeconds * 10

        return total
    }

    fun fullSentence(): String {
        var result = ""
        words.forEach {
            result += it.word + " "
        }
        return result
    }

}