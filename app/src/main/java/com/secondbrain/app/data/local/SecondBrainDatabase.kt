package com.secondbrain.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MemoryEntity::class, RawCaptureEntity::class],
    version = 2,
    exportSchema = true
)
abstract class SecondBrainDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun rawCaptureDao(): RawCaptureDao

    companion object {
        @Volatile
        private var instance: SecondBrainDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memories ADD COLUMN temporalStartDate TEXT")
                db.execSQL("ALTER TABLE memories ADD COLUMN temporalEndDate TEXT")
                db.execSQL("ALTER TABLE memories ADD COLUMN temporalDayPart TEXT")
                db.execSQL("ALTER TABLE memories ADD COLUMN temporalExpression TEXT")
            }
        }

        fun getInstance(context: Context): SecondBrainDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SecondBrainDatabase::class.java,
                    "secondbrain.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
