package com.example.lifegame.ui.focus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lifegame.databinding.ActivityFocusBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FocusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFocusBinding
    
    private var behaviorId: Long = -1
    private var behaviorName: String = ""
    private var focusDuration: Int = 0 // minutes
    
    private var focusService: FocusService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as FocusService.LocalBinder
            focusService = binder.getService()
            isBound = true
            
            // Observe remaining time
            lifecycleScope.launch {
                focusService?.remainingTimeMillis?.collectLatest { remainingMillis ->
                    updateUI(remainingMillis)
                }
            }
            
            // Observe finish state
            lifecycleScope.launch {
                focusService?.isFinished?.collectLatest { finished ->
                    if (finished) {
                        finishFocusSuccess()
                    }
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        behaviorId = intent.getLongExtra("behavior_id", -1L)
        behaviorName = intent.getStringExtra("behavior_name") ?: "未知行动"
        focusDuration = intent.getIntExtra("focus_duration", 0)

        binding.tvBehaviorName.text = behaviorName
        binding.pbCountdown.max = focusDuration * 60 * 1000 // Convert minutes to max millis

        binding.btnGiveUp.setOnClickListener {
            giveUpFocus()
        }

        startAndBindService()
    }

    private fun startAndBindService() {
        val serviceIntent = Intent(this, FocusService::class.java).apply {
            action = FocusService.ACTION_START_FOCUS
            putExtra(FocusService.EXTRA_BEHAVIOR_ID, behaviorId)
            putExtra(FocusService.EXTRA_BEHAVIOR_NAME, behaviorName)
            putExtra(FocusService.EXTRA_DURATION_MINUTES, focusDuration)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun updateUI(remainingMillis: Long) {
        val totalMillis = focusDuration * 60 * 1000L
        if (totalMillis > 0) {
            binding.pbCountdown.progress = remainingMillis.toInt()
        }
        
        val totalSeconds = remainingMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.tvTimeRemaining.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun giveUpFocus() {
        val dialog = AlertDialog.Builder(this, com.example.lifegame.R.style.Theme_LifeGame_Dialog)
            .setTitle("放弃专注")
            .setMessage("确定要放弃本次专注吗？放弃后专注时间将不会记录。")
            .setNegativeButton("取消", null)
            .setPositiveButton("确定放弃") { _, _ ->
                performGiveUpFocus()
            }
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#FF5252"))
    }

    private fun performGiveUpFocus() {
        if (isBound) {
            val stopIntent = Intent(this, FocusService::class.java).apply {
                action = FocusService.ACTION_STOP_FOCUS
            }
            startService(stopIntent)
            unbindService(connection)
            isBound = false
        }
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun finishFocusSuccess() {
        if (isBound) {
            val stopIntent = Intent(this, FocusService::class.java).apply {
                action = FocusService.ACTION_STOP_FOCUS
            }
            startService(stopIntent)
            unbindService(connection)
            isBound = false
        }
        val resultIntent = Intent().apply {
            putExtra("behavior_id", behaviorId)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}