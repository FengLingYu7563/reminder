package com.example.reminder

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class], version = 4, exportSchema = false)
abstract  class AppDatabase: RoomDatabase() {

    abstract  fun noteDao(): NoteDao

    companion object{
        @Volatile
        private  var INSTANCE: AppDatabase? = null

        // v2 → v3：新增貪睡與漸變音量欄位（保留現有資料）
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN snoozeEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE notes ADD COLUMN snoozeMinutes INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE notes ADD COLUMN snoozeMaxCount INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE notes ADD COLUMN snoozeUsedCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN fadeEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE notes ADD COLUMN targetVolume INTEGER NOT NULL DEFAULT 100")
            }
        }

        // v3 → v4：新增個別照片欄位
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN photoPath TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase{
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "note_database"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}
