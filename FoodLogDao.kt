package com.chris.culsi.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FoodLog): Long

    @Update
    suspend fun update(log: FoodLog)

    @Query("SELECT * FROM food_logs WHERE discardedAt IS NULL ORDER BY discardDueAt ASC")
    fun observeActive(): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_logs WHERE discardedAt IS NULL AND discardDueAt >= :cutoff ORDER BY discardDueAt ASC")
    suspend fun getActiveDueAfter(cutoff: Long): List<FoodLog>

    @Query("SELECT * FROM food_logs ORDER BY leftTempAt DESC")
    fun observeAll(): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_logs WHERE leftTempAt >= :cutoff ORDER BY leftTempAt DESC")
    fun observeRecent(cutoff: Long): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_logs WHERE id = :id")
    suspend fun getById(id: Long): FoodLog?

    @Query("SELECT * FROM food_logs WHERE leftTempAt BETWEEN :start AND :end ORDER BY leftTempAt DESC")
    suspend fun getBetween(start: Long, end: Long): List<FoodLog>

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM food_logs WHERE leftTempAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM food_logs WHERE leftTempAt BETWEEN :start AND :end")
    suspend fun deleteBetween(start: Long, end: Long)
}
