package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object ReminderScheduler {

    private const val ACTION_ALARM_FIRE = "com.example.reminder.ALARM_FIRE"
    // 貪睡鬧鐘用獨立 request code，避免覆蓋每日鬧鐘
    private const val SNOOZE_REQ_OFFSET = 1_000_000

    /** 檢查是否可排程精確鬧鐘（API 31+） */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return am.canScheduleExactAlarms()
        }
        return true
    }

    /** 開啟精確鬧鐘設定頁（API 31+，舊版 no-op） */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /** 排程每日鬧鐘（在 note.time 觸發） */
    fun schedule(context: Context, note: Note) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager? ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return
        val pi = buildPendingIntent(context, note.id, note.id, isSnooze = false) ?: return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, note.time, pi)
    }

    /** 排程貪睡鬧鐘（在指定時間觸發，用獨立 request code 與 isSnooze 標記） */
    fun scheduleSnooze(context: Context, noteId: Int, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager? ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return
        val pi = buildPendingIntent(context, noteId + SNOOZE_REQ_OFFSET, noteId, isSnooze = true) ?: return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
    }

    /** 取消指定 noteId 的每日鬧鐘（含貪睡） */
    fun cancel(context: Context, noteId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager? ?: return
        buildPendingIntent(context, noteId, noteId, isSnooze = false)?.let { am.cancel(it); it.cancel() }
        buildPendingIntent(context, noteId + SNOOZE_REQ_OFFSET, noteId, isSnooze = true)?.let { am.cancel(it); it.cancel() }
    }

    /** 只取消貪睡鬧鐘（保留每日鬧鐘） */
    fun cancelSnooze(context: Context, noteId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager? ?: return
        buildPendingIntent(context, noteId + SNOOZE_REQ_OFFSET, noteId, isSnooze = true)?.let { am.cancel(it); it.cancel() }
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        noteId: Int,
        isSnooze: Boolean
    ): PendingIntent? {
        val intent = Intent(ACTION_ALARM_FIRE).apply {
            setPackage(context.packageName)
            putExtra("noteId", noteId)
            putExtra("isSnooze", isSnooze)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
