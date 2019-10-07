package com.example.camerademo.lrc

import android.util.Log
import java.io.*
import java.util.*

class ExtendedLrcBuilder {

    private fun processText(br: File, offset: Double): List<LrcExtendedRow> {
        val rows = ArrayList<LrcExtendedRow>()
        try {
            br.readLines().forEach {line ->
                Log.d(TAG, "lrc raw line: $line")
                if (line.isNotEmpty()) {
                    val lrcRow = LrcExtendedRow.createRow(line, offset.toLong(), rows.size == 0)
                    if (lrcRow != null) {
                        rows.add(lrcRow)
                    }
                }
            }
            if (rows.size > 0) {
                // sort by time:
                rows.sort()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "parse exceptioned: " + e.message)
            throw e
        }

        return rows
    }

    fun getLrcRows(file: File, offset: Double): List<LrcExtendedRow> {
        Log.d(TAG, "getLrcRows by rawString")

        var rows: List<LrcExtendedRow> = ArrayList()
        try {
            rows = processText(file, offset)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return rows
    }

    companion object {
        internal val TAG = "DefaultLrcBuilder"
    }
}
