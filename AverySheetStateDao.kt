package com.chris.culsi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AverySheetStateDao {
    @Query("SELECT * FROM avery_sheet_state WHERE templateId = :templateId LIMIT 1")
    suspend fun get(templateId: String): AverySheetState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: AverySheetState)

    @Query("DELETE FROM avery_sheet_state WHERE templateId = :templateId")
    suspend fun clear(templateId: String)
}
