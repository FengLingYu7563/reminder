package com.example.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao: NoteDao = AppDatabase.getInstance(application).noteDao()
    val notesLiveData: LiveData<List<Note>> = noteDao.getALLNotes()

    fun getNotes() {
        // 無須再調用，LiveData 會自動通知更新
    }
}