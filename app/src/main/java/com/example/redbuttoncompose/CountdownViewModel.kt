package com.example.redbuttoncompose

import android.Manifest
import android.app.Application
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

class CountdownViewModel(application: Application) : AndroidViewModel(application) {

    val context = application.applicationContext
    val isCounting = mutableStateOf(false)
    val currentCount = mutableStateOf(10)

    private var timer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator = context.getSystemService(Vibrator::class.java)

    fun startCountdown() {
        if (isCounting.value) return

        isCounting.value = true
        currentCount.value = 10
        playBeep()

        timer = object : CountDownTimer(10_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                currentCount.value = seconds
                playBeep()
            }

            @RequiresPermission(Manifest.permission.VIBRATE)
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFinish() {
                currentCount.value = 0
                isCounting.value = false
                vibrate()
            }
        }.start()
    }

    fun cancelCountdown() {
        timer?.cancel()
        isCounting.value = false
    }

    private fun playBeep() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, R.raw.beep)
        mediaPlayer?.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onCleared() {
        timer?.cancel()
        mediaPlayer?.release()
        super.onCleared()
    }
}
