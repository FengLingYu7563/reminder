package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 接收 AlarmManager 觸發 → 跳通知 → 重排隔天同時刻
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId", -1)
        if (noteId == -1) return
        val isSnooze = intent.getBooleanExtra("isSnooze", false)

        val pending = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appCtx).noteDao()
                var note = dao.getNoteById(noteId) ?: return@launch
                if (!note.isNotificationEnabled) return@launch

                if (!isSnooze) {
                    // 正常每日觸發：重置貪睡計數，並重排隔天同一時刻
                    val nextTime = computeNextFireTime(note.time)
                    note = note.copy(time = nextTime, snoozeUsedCount = 0)
                    dao.updateNote(note)
                    ReminderScheduler.schedule(appCtx, note)
                }
                // 貪睡再響：不重置、不重排，直接響

                // 啟動前景服務：持續響鈴 + 振動，直到使用者停止
                startAlarmService(appCtx, note)
            } finally {
                pending.finish()
            }
        }
    }

    private fun startAlarmService(context: Context, note: Note) {
        val svc = Intent(context, AlarmService::class.java).apply {
            putExtra("noteId", note.id)
            putExtra("title", note.title)
            putExtra("content", note.content)
            putExtra("snoozeEnabled", note.snoozeEnabled)
            putExtra("snoozeMinutes", note.snoozeMinutes)
            putExtra("snoozeMaxCount", note.snoozeMaxCount)
            putExtra("snoozeUsedCount", note.snoozeUsedCount)
            putExtra("fadeEnabled", note.fadeEnabled)
            putExtra("targetVolume", note.targetVolume)
        }
        // 由精確鬧鐘觸發的廣播有短暫前景啟動豁免
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc)
        } else {
            context.startService(svc)
        }
    }

    private fun computeNextFireTime(currentFire: Long): Long {
        // 從原排程時間加 24 小時直到大於現在
        var t = currentFire + 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        while (t <= now) {
            t += 24L * 60 * 60 * 1000
        }
        return t
    }
}
