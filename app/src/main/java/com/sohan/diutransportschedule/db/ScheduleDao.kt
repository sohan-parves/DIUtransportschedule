package com.sohan.diutransportschedule.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_items ORDER BY routeNo ASC")
    fun observeAll(): Flow<List<DbScheduleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DbScheduleItem>)

    @Query("DELETE FROM schedule_items")
    suspend fun clearAll()
}