package com.secondbrain.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RawCaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(capture: RawCaptureEntity)

    @Query("SELECT * FROM raw_captures WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RawCaptureEntity?
}
