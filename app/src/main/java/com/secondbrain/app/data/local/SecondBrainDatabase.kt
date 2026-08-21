package com.secondbrain.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MemoryEntity::class, RawCaptureEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SecondBrainDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun rawCaptureDao(): RawCaptureDao

    companion object {
        @Volatile
        private var instance: SecondBrainDatabase? = null

        fun getInstance(context: Context): SecondBrainDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SecondBrainDatabase::class.java,
                    "secondbrain.db"
                ).build().also { instance = it }
            }
    }
}
