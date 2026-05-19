package com.example.reminder

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var noteDao: NoteDao
    private var noteId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        noteDao = AppDatabase.getInstance(this).noteDao()

        //if 有 noteId 就 get
        noteId = intent.getIntExtra("noteId", -1).takeIf { it != -1 }

        val ed_content = findViewById<EditText>(R.id.ed_content)
        val ed_title = findViewById<EditText>(R.id.ed_title)
        val btn_save = findViewById<Button>(R.id.btn_save)
        val btn_delete = findViewById<Button>(R.id.btn_delete)
        val btn_back = findViewById<Button>(R.id.btn_back)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)

        //24小時制
        timePicker.setIs24HourView(true)

        // load現有的Note
        noteId?.let { id ->
            lifecycleScope.launch {
                val note = noteDao.getNoteById(id)
                note?.let {
                    ed_title.setText(it.title)
                    ed_content.setText(it.content)
                    // 解析時間戳並load TimePicker
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = it.time
                    }
                    timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
                    timePicker.minute = calendar.get(Calendar.MINUTE)
                }
            }
        }

        btn_save.setOnClickListener {
            val title = ed_title.text.toString()
            val content = ed_content.text.toString()

            // 獲取 TimePicker 的時間並轉換為時間戳
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val timeStamp = calendar.timeInMillis

            // 回 main ，如果有新增或修改
            lifecycleScope.launch {

                if (noteId == null) {
                    //  新增
                    val newNote = Note(
                        title = title,
                        content = content,
                        time = timeStamp,
                        isNotificationEnabled = false
                    )
                    noteDao.insertNote(newNote)
                } else {
                    // 更新
                    val updatedNote = Note(
                        id = noteId!!,
                        title = title,
                        content = content,
                        time = timeStamp,
                        isNotificationEnabled = false
                    )
                    noteDao.updateNote(updatedNote)
                    Log.d("DatabaseUpdate", "Updated note: $timeStamp")
                }
            }
            setResult(RESULT_OK)
            finish()
        }

        //刪除
        btn_delete.setOnClickListener {
            noteId?.let { id ->
                lifecycleScope.launch {
                    val note = noteDao.getNoteById(id)
                    note?.let {
                        noteDao.deleteNote(it)
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }
        }

        //返回
        btn_back.setOnClickListener {
            finish()
        }
    }
}
