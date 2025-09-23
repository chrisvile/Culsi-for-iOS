package com.chris.culsi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val itemName: String,
    val leftTempAt: Long,          // epoch millis
    val discardDueAt: Long,        // epoch millis (computed)
    val discardedAt: Long? = null, // epoch millis
    val discardAction: DiscardAction? = null,
    val loggedBy: String,
    val notes: String? = null
) {
    val isDiscarded: Boolean get() = discardedAt != null
}
