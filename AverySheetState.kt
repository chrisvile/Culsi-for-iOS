package com.chris.culsi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avery_sheet_state")
data class AverySheetState(
    @PrimaryKey val templateId: String,
    val rows: Int,
    val cols: Int,
    // String mask of length rows*cols, '1' = used, '0' = free. Row-major.
    val usedMask: String
)
