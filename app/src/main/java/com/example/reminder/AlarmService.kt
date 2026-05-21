package com.example.reminder

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 前景服務：鬧鐘觸發時持續播放鈴聲(可漸變音量) + 振動，直到使用者按「停止」或「延遲」。
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var fadeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                handleSnooze(intent)
                return START_NOT_STICKY
            }
            ACTION_CANCEL_SNOOZE -> {
                val noteId = intent.getIntExtra("noteId", -1)
                if (noteId != -1) ReminderScheduler.cancelSnooze(applicationContext, noteId)
                NotificationManagerCompat.from(this).cancel(SNOOZE_NOTIF_ID)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // 開始響鈴前先清掉可能殘留的貪睡倒數通知（例如貪睡時間到再響）
        NotificationManagerCompat.from(this).cancel(SNOOZE_NOTIF_ID)

        val noteId = intent?.getIntExtra("noteId", -1) ?: -1
        val title = intent?.getStringExtra("title")?.ifBlank { "提醒" } ?: "提醒"
        val content = intent?.getStringExtra("content") ?: ""
        val snoozeEnabled = intent?.getBooleanExtra("snoozeEnabled", true) ?: true
        val snoozeMinutes = intent?.getIntExtra("snoozeMinutes", 5) ?: 5
        val snoozeMaxCount = intent?.getIntExtra("snoozeMaxCount", -1) ?: -1
        val snoozeUsedCount = intent?.getIntExtra("snoozeUsedCount", 0) ?: 0
        val fadeEnabled = intent?.getBooleanExtra("fadeEnabled", true) ?: true
        val targetVolume = intent?.getIntExtra("targetVolume", 100) ?: 100

        // 是否還能貪睡：開啟 且 (無上限 或 已用次數 < 上限)
        val canSnooze = snoozeEnabled && (snoozeMaxCount == -1 || snoozeUsedCount < snoozeMaxCount)

        startForeground(NOTIF_ID, buildNotification(noteId, title, content, snoozeMinutes, canSnooze))
        startSoundAndVibration(fadeEnabled, targetVolume)
        return START_STICKY
    }

    private fun handleSnooze(intent: Intent) {
        val noteId = intent.getIntExtra("noteId", -1)
        val snoozeMinutes = intent.getIntExtra("snoozeMinutes", 5)
        val title = intent.getStringExtra("title")?.ifBlank { "提醒" } ?: "提醒"
        if (noteId != -1) {
            val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
            ReminderScheduler.scheduleSnooze(applicationContext, noteId, triggerAt)
            showSnoozeCountdown(noteId, title, triggerAt)
            val appCtx = applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                // 已貪睡次數 +1
                val dao = AppDatabase.getInstance(appCtx).noteDao()
                dao.getNoteById(noteId)?.let { n ->
                    dao.updateNote(n.copy(snoozeUsedCount = n.snoozeUsedCount + 1))
                }
            }
        }
        stopAlarm()
    }

    /** 貪睡倒數通知：內建倒數計時 + 「取消延遲」按鈕，可隨時結束 */
    private fun showSnoozeCountdown(noteId: Int, title: String, triggerAt: Long) {
        val cancelPi = PendingIntent.getService(
            this, 4,
            Intent(this, AlarmService::class.java).apply {
                action = ACTION_CANCEL_SNOOZE
                putExtra("noteId", noteId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, ReminderApp.SNOOZE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("貪睡中：$title")
            .setContentText("時間到會再次響鈴")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(triggerAt)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentIntent(cancelPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "關閉鬧鐘", cancelPi)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(SNOOZE_NOTIF_ID, notif)
        } catch (_: SecurityException) {
        }
    }

    private fun buildNotification(
        noteId: Int,
        title: String,
        content: String,
        snoozeMinutes: Int,
        canSnooze: Boolean
    ): android.app.Notification {
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, AlarmService::class.java).apply { action = ACTION_STOP },
            piFlags
        )
        val snoozePi = PendingIntent.getService(
            this, 2,
            Intent(this, AlarmService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra("noteId", noteId)
                putExtra("snoozeMinutes", snoozeMinutes)
                putExtra("title", title)
            },
            piFlags
        )
        val fullScreenPi = PendingIntent.getActivity(
            this, 3,
            Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("noteId", noteId)
                putExtra("title", title)
                putExtra("content", content)
                putExtra("snoozeMinutes", snoozeMinutes)
                putExtra("canSnooze", canSnooze)
            },
            piFlags
        )

        // 自訂版面：按鈕直接在收合(heads-up)樣式顯示
        val customView = RemoteViews(packageName, R.layout.notification_alarm).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_content, content)
            setOnClickPendingIntent(R.id.notif_btn_stop, stopPi)
            if (canSnooze) {
                setViewVisibility(R.id.notif_btn_snooze, android.view.View.VISIBLE)
                setTextViewText(R.id.notif_btn_snooze, "延遲${snoozeMinutes}分鐘")
                setOnClickPendingIntent(R.id.notif_btn_snooze, snoozePi)
            } else {
                setViewVisibility(R.id.notif_btn_snooze, android.view.View.GONE)
            }
        }

        return NotificationCompat.Builder(this, ReminderApp.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setCustomHeadsUpContentView(customView)
            .build()
    }

    private fun startSoundAndVibration(fadeEnabled: Boolean, targetVolume: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxStream = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val curStream = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val startFraction = (curStream.toFloat() / maxStream).coerceIn(0f, 1f)
        val endFraction = (targetVolume / 100f).coerceIn(0f, 1f)

        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                val initVol = if (fadeEnabled) startFraction else endFraction
                setVolume(initVol, initVol)
                start()
            }
        } catch (e: Exception) {
            // 播放失敗不影響振動與通知
        }

        // 漸變音量：30 秒內從 startFraction 線性漸變到 endFraction
        if (fadeEnabled && mediaPlayer != null && startFraction != endFraction) {
            fadeJob = scope.launch {
                val steps = 60
                val stepDelay = FADE_DURATION_MS / steps
                for (i in 1..steps) {
                    delay(stepDelay)
                    val v = (startFraction + (endFraction - startFraction) * i / steps).coerceIn(0f, 1f)
                    try { mediaPlayer?.setVolume(v, v) } catch (_: Exception) { break }
                }
            }
        }

        // 振動：重複波形
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        fadeJob?.cancel()
        fadeJob = null
        try {
            mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
        } catch (_: Exception) {}
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        fadeJob?.cancel()
        stopAlarm()
    }

    companion object {
        const val ACTION_STOP = "com.example.reminder.STOP_ALARM"
        const val ACTION_SNOOZE = "com.example.reminder.SNOOZE_ALARM"
        const val ACTION_CANCEL_SNOOZE = "com.example.reminder.CANCEL_SNOOZE"
        private const val NOTIF_ID = 9999
        private const val SNOOZE_NOTIF_ID = 9998
        private const val FADE_DURATION_MS = 30_000L
    }
}
