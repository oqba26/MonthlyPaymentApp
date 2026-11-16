package com.oqba26.monthlypaymentapp.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var isRecording: Boolean = false
        private set

    fun start(outputFile: File): Boolean {
        return try {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder = newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(16000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            releaseRecorder()
            false
        }
    }

    fun stop(): Boolean {
        val rec = recorder ?: return false
        return try {
            rec.stop()
            true
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Failed to cleanly stop recorder (possibly recording was too short)", e)
            false
        } finally {
            releaseRecorder()
        }
    }

    private fun releaseRecorder() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Error releasing recorder", e)
        }
        recorder = null
        isRecording = false
    }
}
