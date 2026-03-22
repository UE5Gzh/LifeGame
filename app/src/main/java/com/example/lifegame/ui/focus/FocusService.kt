package com.example.lifegame.ui.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var behaviorId: Long = -1
    private var behaviorName: String = ""
    private var totalDurationMillis: Long = 0
    private var startTimeMillis: Long = 0

    private val _remainingTimeMillis = MutableStateFlow(0L)
    val remainingTimeMillis: StateFlow<Long> = _remainingTimeMillis

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    companion object {
        const val CHANNEL_ID = "focus_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_FOCUS = "ACTION_START_FOCUS"
        const val ACTION_STOP_FOCUS = "ACTION_STOP_FOCUS"
        
        const val EXTRA_BEHAVIOR_ID = "EXTRA_BEHAVIOR_ID"
        const val EXTRA_BEHAVIOR_NAME = "EXTRA_BEHAVIOR_NAME"
        const val EXTRA_DURATION_MINUTES = "EXTRA_DURATION_MINUTES"
    }

    inner class LocalBinder : Binder() {
        fun getService(): FocusService = this@FocusService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOCUS -> {
                behaviorId = intent.getLongExtra(EXTRA_BEHAVIOR_ID, -1L)
                behaviorName = intent.getStringExtra(EXTRA_BEHAVIOR_NAME) ?: "专注中"
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
                
                totalDurationMillis = durationMinutes * 60 * 1000L
                startTimeMillis = System.currentTimeMillis()
                
                _isFinished.value = false
                
                startForegroundServiceWithNotification()
                startTimer()
            }
            ACTION_STOP_FOCUS -> {
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                val remaining = totalDurationMillis - elapsedMillis
                
                if (remaining <= 0) {
                    _remainingTimeMillis.value = 0
                    _isFinished.value = true
                    updateNotification("专注完成！", "点击返回应用")
                    break
                } else {
                    _remainingTimeMillis.value = remaining
                    
                    val remainingSeconds = remaining / 1000
                    if (remainingSeconds != lastNotifiedSecond && remainingSeconds % 60 == 0L) {
                        lastNotifiedSecond = remainingSeconds
                        val minutes = remainingSeconds / 60
                        updateNotification("正在专注: $behaviorName", "剩余时间: ${minutes}分钟")
                    }
                }
                delay(100) // Update UI frequently
            }
        }
    }

    private var lastNotifiedSecond: Long = -1

    private fun stopTimer() {
        timerJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "专注倒计时",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示当前专注的剩余时间"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, FocusActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification("正在专注: $behaviorName", "准备开始...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }
}