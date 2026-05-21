package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 開機或 App 更新後重新排程所有開啟提醒的 notes
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pending = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appCtx).noteDao()
                val enabled = dao.getEnabledNotes()
                val now = System.currentTimeMillis()
                for (note in enabled) {
                    // 若儲存的時間已過，往後推 24h 直到未來
                    var nextTime = note.time
                    while (nextTime <= now) {
                        nextTime += 24L * 60 * 60 * 1000
                    }
                    val updated = if (nextTime != note.time) {
                        note.copy(time = nextTime).also { dao.updateNote(it) }
                    } else note
                    ReminderScheduler.schedule(appCtx, updated)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
