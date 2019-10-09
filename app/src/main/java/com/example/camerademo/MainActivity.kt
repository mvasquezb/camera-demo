package com.example.camerademo

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import com.example.camerademo.camera2.events.CameraEventListener
import com.example.camerademo.camera2.events.Error
import com.example.camerademo.lrc.LrcFile
import com.example.camerademo.lrc.LrcRow
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        @JvmStatic val TAG = MainActivity::class.java.simpleName
    }

    private var lrcRemainingTime: Long = 0
    private var lyricsTimer: CountDownTimer? = null
    private lateinit var lrcFile: LrcFile
    private var rowIndex: Int = 0
    private var wordIndex: Int = 0
    private var isRecording = false
    private lateinit var viewModel: MainViewModel
    private var songPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        setupVideoRecording()
        setupSongPlayer()
        setupLrcPlayer()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        viewModel.loading.observe(this, Observer<Boolean> { loading ->
            when (loading) {
                true -> loadingBar.visibility = View.VISIBLE
                else -> loadingBar.visibility = View.GONE
            }
        })
        viewModel.previewReady.observe(this, Observer<Boolean> {
            when (it) {
                true -> {
                    val intent = Intent(this, PreviewActivity::class.java).apply {
                        putExtra("video", viewModel.savedPath)
                        putExtra("saveDir", viewModel.saveDir.absolutePath)
                    }
                    startActivity(intent)
                }
            }
        })
        viewModel.lrcFile.observe(this, Observer {lrc ->
            previousLyric.text = ""
            highlightedLyric.text = lrc.rows[rowIndex].fullSentence()
            nextLyric.text = lrc.rows[rowIndex + 1].fullSentence()
            lrcFile = lrc
        })
    }

    private fun setupLrcPlayer() {
        viewModel.downloadLrcFile()
    }

    private fun setupSongPlayer() {
        songPlayer = MediaPlayer().apply {
            setDataSource(viewModel.song.mp3)
            prepare()
            if (viewModel.currentPosition != 0) {
                seekTo(viewModel.currentPosition)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        camera.start()
        setupSongPlayer()
    }

    override fun onPause() {
        onRecordingStop()
        super.onPause()
    }

    private fun stopSongPlayer(reset: Boolean = true) {
        songPlayer?.runCatching {
            viewModel.currentPosition = currentPosition
            release()
        }
        songPlayer = null
    }

    private fun pauseSongPlayer() {
        songPlayer?.runCatching {
            viewModel.currentPosition = currentPosition
            pause()
        }
    }

    private fun startSongPlayer() {
        songPlayer?.runCatching {
            start()
        }
    }

    private fun showToast(message : String) = Toast.makeText(this, message, LENGTH_SHORT).show()

    private fun setupVideoRecording() {
        playButton.setOnClickListener {
            toggleRecording()
        }
        finishButton.setOnClickListener {
            onRecordingFinish()
        }
        camera.addEventListener(object : CameraEventListener {
            override fun onError(error: Error) {
                Log.e(TAG, "error: ${error.error?.message}")
                showToast(error.error?.message ?: "Error connecting to camera")
            }
        })
    }

    private fun toggleRecording() {
        if (!isRecording) {
            onRecordingStart()
        } else {
            onRecordingPause()
        }
    }

    private fun uiStartRecording() {
        isRecording = true
        playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_pause))
    }

    private fun uiPauseRecording() {
        isRecording = false
        playButton.setImageDrawable(getDrawable(android.R.drawable.ic_media_play))
    }

    private fun onRecordingStart() {
        uiStartRecording()
        startSongPlayer()
        startLrcPlayer()
        camera.startVideo { video ->
            Log.d(TAG, "video callback: ${video.absolutePath}")
            lifecycleScope.launch {
                showToast("Video saved: ${viewModel.getVideoFilePath()}")
                viewModel.handleVideo(video)
            }
        }
    }

    /** START LRC Player time **/
    private fun startLrcPlayer() {
        if (lrcRemainingTime == 0L) {
            lrcRemainingTime = songPlayer?.duration?.toLong() ?: 0
        }
        lyricsTimer = object : CountDownTimer(lrcRemainingTime, 10) {
            override fun onFinish() {
                Log.e("TimerTesting", "Time ended")
            }

            override fun onTick(millisUntilFinished: Long) {
                if (isNextWordTime(lrcFile, rowIndex, wordIndex, millisUntilFinished)) {
                    lrcRemainingTime = millisUntilFinished
                    changeToNextWord(lrcFile.rows[rowIndex], wordIndex)
                    wordIndex++
                } else if (isNextRowTime(lrcFile, rowIndex, millisUntilFinished)) {
                    changeToNextRow(lrcFile, rowIndex)
                    rowIndex++
                    wordIndex = 0
                }
            }
        }.start()
    }

    private fun isNextWordTime(lrcFile: LrcFile, rowIndex: Int, wordIndex: Int, millisUntilFinished: Long): Boolean {
        if (wordIndex == lrcFile.rows[rowIndex].words.size) {
            return false
        }
        val currentTime = lrcFile.timeLength - millisUntilFinished
        val nextWordTime = lrcFile.rows[rowIndex].words[wordIndex].endTime.toLong()
        return currentTime >= nextWordTime
    }

    private fun changeToNextWord(lrcRow: LrcRow, wordIndex: Int) {
        val coloredSentence = lrcRow.words
            .take(wordIndex + 1)
            .joinToString(" ") { it.word }
        Log.d(TAG, "colored sentence: $coloredSentence")
        val spannableString = SpannableString(lrcRow.fullSentence())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            spannableString.setSpan(
                ForegroundColorSpan(getColor(R.color.colorPrimaryDark)),
                0,
                coloredSentence.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            spannableString.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)),
                0,
                coloredSentence.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        Log.e("TimerTesting", "Colored sentence $coloredSentence")
        highlightedLyric.text = spannableString
    }

    private fun isNextRowTime(lrcFile: LrcFile, rowIndex: Int, millisUntilFinished: Long) =
        rowIndex + 1 < lrcFile.rows.size &&
        lrcFile.timeLength - millisUntilFinished >= lrcFile.rows[rowIndex + 1].startTime.toLong()

    private fun changeToNextRow(lrcFile: LrcFile, index: Int) {
        val rowsLeft = lrcFile.rows.size - index - 1
        if (rowsLeft >= 1) {
            previousLyric.text = lrcFile.rows[index].fullSentence()
            highlightedLyric.text = lrcFile.rows[index + 1].fullSentence()
        }
        if (rowsLeft >= 2) {
            nextLyric.text = lrcFile.rows[index + 2].fullSentence()
        } else {
            nextLyric.text = ""
        }
    }

    private fun stopLrcPlayer() {
        lyricsTimer?.cancel()
    }
    /** END LRC Player time **/

    private fun onRecordingPause() {
        uiPauseRecording()
        camera.stopVideo()
        pauseSongPlayer()
        stopLrcPlayer()
    }

    private fun onRecordingStop() {
        uiPauseRecording()
        camera.stop()
        stopSongPlayer()
        stopLrcPlayer()
    }

    private fun onRecordingFinish() {
        if (isRecording) {
            onRecordingStop()
        }
        lifecycleScope.launch {
            viewModel.processVideos()
        }
    }
}
