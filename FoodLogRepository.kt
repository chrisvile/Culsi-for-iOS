package com.chris.culsi.data

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class FoodLogRepository(private val dao: FoodLogDao) {
    fun active(): Flow<List<FoodLog>> = dao.observeActive()
    fun all(): Flow<List<FoodLog>> = dao.observeAll()
    fun recent(days: Int): Flow<List<FoodLog>> {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return dao.observeRecent(cutoff)
    }

    suspend fun purgeOldLogs(retainDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retainDays.toLong())
        dao.deleteOlderThan(cutoff)
    }

    suspend fun add(
        itemName: String,
        leftTempAt: Long,
        durationMillis: Long,
        loggedBy: String,
        notes: String?
    ): Long {
        val due = leftTempAt + durationMillis
        return dao.insert(
            FoodLog(
                itemName = itemName.trim(),
                leftTempAt = leftTempAt,
                discardDueAt = due,
                loggedBy = loggedBy.trim(),
                notes = notes?.trim()
            )
        )
    }

    suspend fun markDiscarded(
        id: Long,
        atMillis: Long,
        action: DiscardAction
    ) {
        val current = dao.getById(id) ?: return
        dao.update(current.copy(discardedAt = atMillis, discardAction = action))
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clear(start: Long, end: Long) {
        dao.deleteBetween(start, end)
    }

    suspend fun export(start: Long, end: Long, format: ExportFormat): String {
        val logs = dao.getBetween(start, end)
        return when (format) {
            ExportFormat.CSV -> logs.toCsv()
            ExportFormat.JSON -> logs.toJson()
        }
    }

    private fun List<FoodLog>.toCsv(): String = buildString {
        appendLine("id,itemName,leftTempAt,discardDueAt,discardedAt,discardAction,loggedBy,notes")
        for (log in this@toCsv) {
            append(log.id).append(',')
            append('"').append(log.itemName.escapeCsv()).append('"').append(',')
            append(log.leftTempAt).append(',')
            append(log.discardDueAt).append(',')
            append(log.discardedAt ?: "").append(',')
            append(log.discardAction ?: "").append(',')
            append('"').append(log.loggedBy.escapeCsv()).append('"').append(',')
            append('"').append(log.notes?.escapeCsv() ?: "").append('"')
            appendLine()
        }
    }

    private fun List<FoodLog>.toJson(): String = buildString {
        append('[')
        this@toJson.forEachIndexed { index, log ->
            if (index > 0) append(',')
            append('{')
            append("\"id\":${log.id},")
            append("\"itemName\":\"${log.itemName.escapeJson()}\",")
            append("\"leftTempAt\":${log.leftTempAt},")
            append("\"discardDueAt\":${log.discardDueAt},")
            append("\"discardedAt\":${log.discardedAt ?: "null"},")
            append("\"discardAction\":${log.discardAction?.let { "\"$it\"" } ?: "null"},")
            append("\"loggedBy\":\"${log.loggedBy.escapeJson()}\",")
            append("\"notes\":${log.notes?.let { "\"${it.escapeJson()}\"" } ?: "null"}")
            append('}')
        }
        append(']')
    }

    private fun String.escapeCsv(): String = replace("\"", "\"\"")
    private fun String.escapeJson(): String = replace("\"", "\\\"")
}
