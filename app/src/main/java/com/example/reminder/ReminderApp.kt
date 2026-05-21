package com.example.reminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ReminderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 建立通知頻道（API 26+ 必須）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // 一般提醒通知頻道
            val channel = NotificationChannel(
                CHANNEL_ID,
                "提醒通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "提醒事項到時跳出通知"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)

            // 鬧鐘前景服務頻道：本身不發聲（鈴聲由 AlarmService 循環播放），避免雙重音效
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "鬧鐘響鈴",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "提醒到時持續響鈴的鬧鐘"
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(alarmChannel)

            // 貪睡倒數頻道：常駐、無聲、不彈出 heads-up
            val snoozeChannel = NotificationChannel(
                SNOOZE_CHANNEL_ID,
                "貪睡倒數",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "貪睡期間顯示倒數，可隨時取消"
                setSound(null, null)
            }
            manager.createNotificationChannel(snoozeChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val SNOOZE_CHANNEL_ID = "snooze_channel"
    }
}
