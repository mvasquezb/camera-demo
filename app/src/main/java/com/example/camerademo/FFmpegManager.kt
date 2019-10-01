package com.example.camerademo

import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File

object FFmpegManager {
    fun concat(filePaths: List<String>, saveDir: File): File {
        // Add input files
        val args = mutableListOf<String>().apply{
            addAll(arrayOf(
                "-f",
                "concat",
                "-safe",
                "0"
            ))
        }
        val tmpFilePaths = File(saveDir, "lists.txt")
        val outStream = tmpFilePaths.outputStream()
        val writer = outStream.writer()
        filePaths.forEach {
            writer.write("file $it\n")
        }
        writer.close()
        outStream.flush()
        outStream.close()
        args.apply {
            add("-i")
            add(tmpFilePaths.absolutePath)
            add("-c")
            add("copy")
            add("-y")
        }

        // Temporary output file
        val tempFile = createTempFile(suffix = ".mp4", directory = saveDir)
        args.add(tempFile.absolutePath)

        FFmpeg.execute(args.toTypedArray())
        return tempFile
    }
}