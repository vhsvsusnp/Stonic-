package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StonicDatabase : RoomDatabase() {

    abstract fun stonicDao(): StonicDao

    companion object {
        @Volatile
        private var INSTANCE: StonicDatabase? = null

        fun getInstance(context: Context): StonicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StonicDatabase::class.java,
                    "stonic_ai_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
