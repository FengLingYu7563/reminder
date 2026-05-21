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
    @ColumnInfo (name = "isNotificationEnabled") var isNotificationEnabled: Boolean,
    // 貪睡設定（每筆鬧鐘各自獨立）
    @ColumnInfo(name = "snoozeEnabled") var snoozeEnabled: Boolean = true,
    @ColumnInfo(name = "snoozeMinutes") var snoozeMinutes: Int = 5,
    @ColumnInfo(name = "snoozeMaxCount") var snoozeMaxCount: Int = -1, // -1 = 無上限
    @ColumnInfo(name = "snoozeUsedCount") var snoozeUsedCount: Int = 0, // 已貪睡次數（執行期計數）
    // 漸變音量設定
    @ColumnInfo(name = "fadeEnabled") var fadeEnabled: Boolean = true,
    @ColumnInfo(name = "targetVolume") var targetVolume: Int = 100, // 目標音量 0-100%
    // 個別照片（檔名，存在 filesDir；空字串=無）
    @ColumnInfo(name = "photoPath") var photoPath: String = ""
)