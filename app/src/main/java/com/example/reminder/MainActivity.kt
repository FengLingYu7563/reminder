package com.example.reminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()

        //新增
        findViewById<Button>(R.id.btn_new).setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            startActivity(intent)
        }

        noteDao = AppDatabase.getInstance(this).noteDao()

        //轉畫面
        val btn_new = findViewById<Button>(R.id.btn_new)
        btn_new.setOnClickListener{
            val intent = Intent(this, DetailActivity::class.java)
            resultLauncher.launch(intent)

        }

        // 觀察 LiveData ，自動更新 RecyclerView
        noteViewModel.notesLiveData.observe(this, Observer { notes ->
            adapter.updateReminders(notes)
        })
    }

    // 初始化 RecyclerView 和 Adpter
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ReminderAdapter(notes, { note, isChecked ->
            lifecycleScope.launch {
                note.isNotificationEnabled = isChecked
                noteDao.updateNote(note)
            }
        }, { note ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("noteId", note.id)
            startActivity(intent)
        })

        recyclerView.adapter = adapter
    }


}