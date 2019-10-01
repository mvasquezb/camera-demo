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

    fun muxVideoAudio(video: File, audio: File, saveDir: File): File {
        val args = mutableListOf<String>().apply {
            add("-i")
            add(video.absolutePath)
            add("-i")
            add(audio.absolutePath)
            add("-c")
            add("copy")
            add("-shortest")
        }
        val outFile = createTempFile(directory = saveDir, suffix = ".mp4")
        args.add(outFile.absolutePath)
        FFmpeg.execute(args.toTypedArray())
        return outFile
    }

    fun demuxVideoAudio(video: File, saveDir: File): Map<String, File> {
        val audioTrack = createTempFile(directory = saveDir, suffix = ".m4a")
        val videoTrack = createTempFile(directory = saveDir, suffix = ".mp4")

        val args = mutableListOf<String>().apply {
            add("-i")
            add(video.absolutePath)
            add("-c:v")
            add("copy")
            add("-map")
            add("0:0")
            add(videoTrack.absolutePath)
            add("-c:a")
            add("copy")
            add("-map")
            add("0:1")
            add(audioTrack.absolutePath)
        }
        FFmpeg.execute(args.toTypedArray())
        return mapOf(
            "video" to videoTrack,
            "audio" to audioTrack
        )
    }
}