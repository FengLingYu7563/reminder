package com.example.reminder

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey (autoGenerate = true)val id: Int = 0,
    @ColumnInfo (name = "title") val title: String,
    @ColumnInfo (name = "content") val content: String,
    @ColumnInfo (name = "time") val time: Long,
    @ColumnInfo (name = "isNotificationEnabled") var isNotificationEnabled: Boolean
)