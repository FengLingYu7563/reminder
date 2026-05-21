package com.example.reminder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全螢幕鬧鐘畫面：響鈴時跳出（鎖屏也會亮屏顯示），提供「停止」與「延遲5分鐘」。
 */
class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 鎖屏顯示 + 亮屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(R.layout.activity_alarm)

        val noteId = intent.getIntExtra("noteId", -1)
        val title = intent.getStringExtra("title")?.ifBlank { "提醒" } ?: "提醒"
        val content = intent.getStringExtra("content") ?: ""
        val snoozeMinutes = intent.getIntExtra("snoozeMinutes", 5)
        val canSnooze = intent.getBooleanExtra("canSnooze", true)

        findViewById<TextView>(R.id.alarm_time).text =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        findViewById<TextView>(R.id.alarm_title).text = title
        findViewById<TextView>(R.id.alarm_content).text = content

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            sendServiceAction(AlarmService.ACTION_STOP, noteId, snoozeMinutes)
            finish()
        }

        val snoozeBtn = findViewById<Button>(R.id.btn_snooze)
        if (canSnooze) {
            snoozeBtn.text = "延遲 ${snoozeMinutes} 分鐘"
            snoozeBtn.setOnClickListener {
                sendServiceAction(AlarmService.ACTION_SNOOZE, noteId, snoozeMinutes, title)
                finish()
            }
        } else {
            snoozeBtn.visibility = android.view.View.GONE
        }
    }

    private fun sendServiceAction(action: String, noteId: Int, snoozeMinutes: Int, title: String = "") {
        val intent = Intent(this, AlarmService::class.java).apply {
            this.action = action
            putExtra("noteId", noteId)
            putExtra("snoozeMinutes", snoozeMinutes)
            putExtra("title", title)
        }
        startService(intent)
    }

    // 按返回視同停止（避免誤關卻仍在響鈴）
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        sendServiceAction(AlarmService.ACTION_STOP, -1, 5)
        super.onBackPressed()
    }
}
