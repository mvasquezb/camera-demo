package com.example.camerademo.lrc

import android.util.Log
import java.util.ArrayList

class LrcExtendedRow(
    var times: List<Long>,
    /**
     * begin time of this lrc row
     */
    var initialTime: Long,
    /**
     * content of this lrc
     */
    var content: List<String>,
    var strTime: String,
    var fullSentence: String,
    var lyricType: Int?
) : Comparable<LrcExtendedRow> {

    override fun compareTo(another: LrcExtendedRow): Int {
        return (this.initialTime - another.initialTime).toInt()
    }

    companion object {
        val TAG = "LrcRow"
        val MY_PART = 1

        /**
         * create LrcRows by standard Lrc Line , if not standard lrc line,
         * return false<br></br>
         * [00:00:20] balabalabalabala
         */
        fun createRow(
            standardLrcLine: String,
            offset: Long,
            uppercaseFirstWord: Boolean
        ): LrcExtendedRow? {
            try {
                if (standardLrcLine.indexOf("[") != 0 || standardLrcLine.indexOf("]") != 9) {
                    if (standardLrcLine.indexOf("[") != 2 || standardLrcLine.indexOf("]") != 11) {
                        return null
                    }
                }
                var lyricType = MY_PART
                if (standardLrcLine.indexOf("[") == 2) {
                    lyricType = Integer.valueOf(standardLrcLine.substring(0, 1))
                }

                val lastIndexOfRightBracket = standardLrcLine.indexOf("]")
                val content = ArrayList<String>()
                val times = ArrayList<Long>()
                val remaining = standardLrcLine.substring(lastIndexOfRightBracket + 1)

                val strTime = standardLrcLine.substring(
                    standardLrcLine.indexOf("[") + 1,
                    lastIndexOfRightBracket
                )
                val time = timeConvert(strTime) - offset

                var fullSentence = ""
                val words =
                    remaining.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in words.indices) {
                    words[i] = words[i].toLowerCase()
                }
                if (uppercaseFirstWord && words.size > 0) {
                    words[0] = words[0].substring(0, 1).toUpperCase() + words[0].substring(1)
                }

                times.add(time)

                var previousWord = ""
                for (fullWord in words) {
                    val indexOfRightMark = fullWord.indexOf(">")
                    if (indexOfRightMark == -1) {
                        if (previousWord != "")
                            previousWord += " "
                        previousWord += fullWord
                        continue
                    }
                    val phrases =
                        fullWord.split(">".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    if (fullSentence.length != 0)
                        fullSentence += " "

                    /*if(phrases.length > 1)
                    Log.i("LRC", "Edge case");*/

                    for (s in phrases) {
                        val pair =
                            s.split("<".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                        var word = pair[0]
                        if (previousWord != "") {
                            word = "$previousWord $word"
                            previousWord = ""
                        }

                        val newTime = timeConvert(pair[1]) - offset
                        fullSentence += word
                        times.add(newTime)
                        content.add(word)
                    }
                }


                return LrcExtendedRow(times, time, content, strTime, fullSentence, lyricType)
            } catch (e: Exception) {
                Log.e(TAG, "createRows exception:" + e.message)
                return null
            }

        }

        private fun timeConvert(timeString: String): Long {
            var timeString = timeString
            timeString = timeString.replace('.', ':')
            val times =
                timeString.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            // mm:ss:SS
            return (Integer.valueOf(times[0]) * 60 * 1000 +
                    Integer.valueOf(times[1]) * 1000 +
                    Integer.valueOf(times[2]) * 10).toLong()
        }
    }
}