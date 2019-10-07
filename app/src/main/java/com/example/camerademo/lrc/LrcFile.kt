package com.example.camerademo.lrc

import java.io.File

class LrcFile(file: File) {

    lateinit var artist: String
    lateinit var name: String
    var timeLength: Int = 0
    var rows: MutableList<LrcRow> = mutableListOf()

    init {
        var index = 0
        file.forEachLine {
            when (index) {
                0 -> name = it.substring(it.indexOf("[ar:") + 4, it.indexOf("]"))
                1 -> artist = it.substring(it.indexOf("[ti:") + 4, it.indexOf("]"))
                2 -> timeLength = stringToMillisecond(
                        it.substring(it.indexOf("[length ") + 8, it.indexOf("]"))
                )
                else -> {
                    rows.add(LrcRow(it))
                }
            }
            index++
        }
    }

    private fun stringToMillisecond(timeAsString: String): Int {

        var total = 0

        val min = timeAsString.substring(0, timeAsString.indexOf(":")).toInt()
        total += min * 60 * 1000

        val seconds = timeAsString.substring(timeAsString.indexOf(":") + 1).toInt()
        total += seconds * 1000

        val centiSecondsIndex = timeAsString.indexOf(".")
        if (centiSecondsIndex != -1) {
            val centiSeconds = timeAsString.substring(centiSecondsIndex).toInt()
            total += centiSeconds * 10
        }

        return total
    }

}