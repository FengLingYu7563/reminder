package com.example.reminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter

    private lateinit var noteDao: NoteDao
    private val notes = mutableListOf<Note>()

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {  result ->
        if (result.resultCode == RESULT_OK) {
            //loadNotes
        }
    }

    // 通知權限請求（API 33+）
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 結果忽略；不給就無法跳通知，後續仍可由使用者到設定開啟 */ }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 套用自訂背景（若有）
        BackgroundManager.applyTo(findViewById(R.id.main))

        setupRecyclerView()

        noteDao = AppDatabase.getInstance(this).noteDao()

        //新增（右下角）
        findViewById<View>(R.id.btn_new).setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            resultLauncher.launch(intent)
        }

        // 設定（左下角）
        findViewById<View>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 觀察 LiveData ，自動更新 RecyclerView
        noteViewModel.notesLiveData.observe(this, Observer { notes ->
            adapter.updateReminders(notes)
        })

        // 請求通知 / 精確鬧鐘權限
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // 從設定頁返回時刷新背景
        BackgroundManager.applyTo(findViewById(R.id.main))
    }

    // 初始化 RecyclerView 和 Adpter
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ReminderAdapter(notes, { note, isChecked ->
            lifecycleScope.launch {
                // 切換開關時：DB 寫入 + 排程／取消
                val toggled = note.copy(isNotificationEnabled = isChecked)
                if (isChecked) {
                    // 若儲存時間已過，往後推到下個未來時刻再排
                    val nextTime = nextFutureTime(toggled.time)
                    val finalNote = toggled.copy(time = nextTime)
                    noteDao.updateNote(finalNote)
                    if (!ReminderScheduler.canScheduleExact(this@MainActivity)) {
                        showExactAlarmDialog()
                    } else {
                        ReminderScheduler.schedule(this@MainActivity, finalNote)
                    }
                } else {
                    noteDao.updateNote(toggled)
                    ReminderScheduler.cancel(this@MainActivity, toggled.id)
                }
            }
        }, { note ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("noteId", note.id)
            startActivity(intent)
        })

        recyclerView.adapter = adapter
    }

    private fun nextFutureTime(t: Long): Long {
        val now = System.currentTimeMillis()
        if (t > now) return t
        // 用 Calendar 取 HH:mm 後跳到明天的同時刻
        val cal = Calendar.getInstance().apply { timeInMillis = t }
        val hh = cal.get(Calendar.HOUR_OF_DAY)
        val mm = cal.get(Calendar.MINUTE)
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hh)
            set(Calendar.MINUTE, mm)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (!ReminderScheduler.canScheduleExact(this)) {
            showExactAlarmDialog()
        }
    }

    private fun showExactAlarmDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要精確鬧鐘權限")
            .setMessage("為了讓提醒準時觸發，請到設定中啟用「鬧鐘與提醒」權限。")
            .setPositiveButton("前往設定") { _, _ ->
                ReminderScheduler.openExactAlarmSettings(this)
            }
            .setNegativeButton("稍後", null)
            .show()
    }
}
