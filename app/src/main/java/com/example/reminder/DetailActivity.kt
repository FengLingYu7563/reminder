package com.example.reminder

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class DetailActivity : AppCompatActivity() {

    private lateinit var noteDao: NoteDao
    private var noteId: Int? = null
    // 編輯既有 note 時保留原本的提醒開關狀態
    private var existingNotificationEnabled: Boolean = false
    private var existingSnoozeUsedCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        noteDao = AppDatabase.getInstance(this).noteDao()
        noteId = intent.getIntExtra("noteId", -1).takeIf { it != -1 }

        val ed_content = findViewById<EditText>(R.id.ed_content)
        val ed_title = findViewById<EditText>(R.id.ed_title)
        val btn_save = findViewById<Button>(R.id.btn_save)
        val btn_delete = findViewById<Button>(R.id.btn_delete)
        val btn_back = findViewById<Button>(R.id.btn_back)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)

        val switchSnooze = findViewById<SwitchCompat>(R.id.switch_snooze)
        val snoozeOptions = findViewById<View>(R.id.snooze_options)
        val spinnerMinutes = findViewById<Spinner>(R.id.spinner_snooze_minutes)
        val spinnerCount = findViewById<Spinner>(R.id.spinner_snooze_count)
        val switchFade = findViewById<SwitchCompat>(R.id.switch_fade)
        val fadeOptions = findViewById<View>(R.id.fade_options)
        val seekVolume = findViewById<SeekBar>(R.id.seekbar_volume)
        val tvVolume = findViewById<TextView>(R.id.tv_volume_value)

        timePicker.setIs24HourView(true)

        // 下拉選單內容
        spinnerMinutes.adapter = ArrayAdapter.createFromResource(
            this, R.array.snooze_minutes_entries, android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerCount.adapter = ArrayAdapter.createFromResource(
            this, R.array.snooze_count_entries, android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // 群組開關顯隱
        switchSnooze.setOnCheckedChangeListener { _, isChecked ->
            snoozeOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        switchFade.setOnCheckedChangeListener { _, isChecked ->
            fadeOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVolume.text = "$progress%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // 預設值（新增時）
        applyDefaults(switchSnooze, snoozeOptions, spinnerMinutes, spinnerCount,
            switchFade, fadeOptions, seekVolume, tvVolume)

        // load現有的Note
        noteId?.let { id ->
            lifecycleScope.launch {
                val note = noteDao.getNoteById(id)
                note?.let {
                    ed_title.setText(it.title)
                    ed_content.setText(it.content)
                    existingNotificationEnabled = it.isNotificationEnabled
                    existingSnoozeUsedCount = it.snoozeUsedCount
                    val calendar = Calendar.getInstance().apply { timeInMillis = it.time }
                    timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
                    timePicker.minute = calendar.get(Calendar.MINUTE)

                    // 套用既有貪睡/音量設定
                    switchSnooze.isChecked = it.snoozeEnabled
                    snoozeOptions.visibility = if (it.snoozeEnabled) View.VISIBLE else View.GONE
                    spinnerMinutes.setSelection((it.snoozeMinutes - 1).coerceIn(0, 9))
                    spinnerCount.setSelection(countValueToIndex(it.snoozeMaxCount))
                    switchFade.isChecked = it.fadeEnabled
                    fadeOptions.visibility = if (it.fadeEnabled) View.VISIBLE else View.GONE
                    seekVolume.progress = it.targetVolume
                    tvVolume.text = "${it.targetVolume}%"
                }
            }
        }

        btn_save.setOnClickListener {
            val title = ed_title.text.toString()
            val content = ed_content.text.toString()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val timeStamp = calendar.timeInMillis

            val snoozeEnabled = switchSnooze.isChecked
            val snoozeMinutes = spinnerMinutes.selectedItemPosition + 1
            val snoozeMaxCount = countIndexToValue(spinnerCount.selectedItemPosition)
            val fadeEnabled = switchFade.isChecked
            val targetVolume = seekVolume.progress

            lifecycleScope.launch {
                val savedNote: Note = if (noteId == null) {
                    val newNote = Note(
                        title = title, content = content, time = timeStamp,
                        isNotificationEnabled = false,
                        snoozeEnabled = snoozeEnabled, snoozeMinutes = snoozeMinutes,
                        snoozeMaxCount = snoozeMaxCount, snoozeUsedCount = 0,
                        fadeEnabled = fadeEnabled, targetVolume = targetVolume
                    )
                    val newId = noteDao.insertNote(newNote).toInt()
                    newNote.copy(id = newId)
                } else {
                    val updatedNote = Note(
                        id = noteId!!, title = title, content = content, time = timeStamp,
                        isNotificationEnabled = existingNotificationEnabled,
                        snoozeEnabled = snoozeEnabled, snoozeMinutes = snoozeMinutes,
                        snoozeMaxCount = snoozeMaxCount, snoozeUsedCount = existingSnoozeUsedCount,
                        fadeEnabled = fadeEnabled, targetVolume = targetVolume
                    )
                    noteDao.updateNote(updatedNote)
                    Log.d("DatabaseUpdate", "Updated note: $timeStamp")
                    updatedNote
                }

                // 提醒開啟才重新排程
                if (savedNote.isNotificationEnabled) {
                    if (!ReminderScheduler.canScheduleExact(this@DetailActivity)) {
                        showExactAlarmDialog()
                    } else {
                        ReminderScheduler.schedule(this@DetailActivity, savedNote)
                    }
                }

                setResult(RESULT_OK)
                finish()
            }
        }

        btn_delete.setOnClickListener {
            noteId?.let { id ->
                lifecycleScope.launch {
                    val note = noteDao.getNoteById(id)
                    note?.let {
                        ReminderScheduler.cancel(this@DetailActivity, it.id)
                        noteDao.deleteNote(it)
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }
        }

        btn_back.setOnClickListener { finish() }
    }

    private fun applyDefaults(
        switchSnooze: SwitchCompat, snoozeOptions: View,
        spinnerMinutes: Spinner, spinnerCount: Spinner,
        switchFade: SwitchCompat, fadeOptions: View,
        seekVolume: SeekBar, tvVolume: TextView
    ) {
        // 貪睡預設：開、5分鐘、無上限
        switchSnooze.isChecked = true
        snoozeOptions.visibility = View.VISIBLE
        spinnerMinutes.setSelection(4) // 5 分鐘
        spinnerCount.setSelection(5)    // 無上限
        // 漸變音量預設：開、目標 100%
        switchFade.isChecked = true
        fadeOptions.visibility = View.VISIBLE
        seekVolume.progress = 100
        tvVolume.text = "100%"
    }

    // 次數：index 0-4 → 1-5，index 5 → -1(無上限)
    private fun countIndexToValue(index: Int): Int = if (index >= 5) -1 else index + 1
    private fun countValueToIndex(value: Int): Int = if (value == -1) 5 else (value - 1).coerceIn(0, 4)

    private fun showExactAlarmDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要精確鬧鐘權限")
            .setMessage("為了讓提醒準時觸發，請到設定中啟用「鬧鐘與提醒」權限。")
            .setPositiveButton("前往設定") { _, _ ->
                ReminderScheduler.openExactAlarmSettings(this)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
