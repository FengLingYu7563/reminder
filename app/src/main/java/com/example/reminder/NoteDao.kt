package com.example.reminder

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface NoteDao {
    @Insert
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE title = :title LIMIT 1")
    suspend fun getNoteByName(title: String): Note?

    @Query("SELECT * FROM notes")
    fun getALLNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :userId")
    suspend fun getNoteById(userId: Int): Note?

}
