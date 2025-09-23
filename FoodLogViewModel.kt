package com.chris.culsi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chris.culsi.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * NOTE: Don’t hardcode regulations. Make duration configurable.
 * Default below uses the common 4-hour TPHC window.
 */
class FoodLogViewModel(app: Application) : AndroidViewModel(app) {

    // Default TPHC durations (configurable later in Settings UI)
    var defaultDuration: Duration = 4.hours
        private set

    private val repo = FoodLogRepository(CulsiDb.get(app).foodLogDao())

    init {
        viewModelScope.launch { repo.purgeOldLogs() }
    }

    val active = repo.active().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val all = repo.all().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val recent = repo.recent(30).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun createLog(
        itemName: String,
        leftTempAt: Long,
        durationMillis: Long? = null,
        loggedBy: String,
        notes: String?
    ) = viewModelScope.launch {
        val dur = durationMillis ?: defaultDuration.inWholeMilliseconds
        val appCtx = getApplication<Application>()
        val id = repo.add(itemName, leftTempAt, dur, loggedBy, notes)
        com.chris.culsi.alerts.AlarmScheduler.scheduleDueAlarm(
            appCtx, id, itemName, leftTempAt + dur
        )
    }

    fun discard(id: Long, action: DiscardAction) = viewModelScope.launch {
        repo.markDiscarded(id, System.currentTimeMillis(), action)
        val appCtx = getApplication<Application>()
        com.chris.culsi.alerts.AlarmScheduler.cancelDueAlarm(appCtx, id)
    }

    fun delete(id: Long) = viewModelScope.launch {
        val appCtx = getApplication<Application>()
        com.chris.culsi.alerts.AlarmScheduler.cancelDueAlarm(appCtx, id)
        repo.delete(id)
    }

    fun clearLogs(start: Long, end: Long) = viewModelScope.launch {
        repo.clear(start, end)
    }

    suspend fun exportLogs(start: Long, end: Long, format: ExportFormat): String {
        return repo.export(start, end, format)
    }

    fun setDefaultDurationHours(hours: Int) {
        defaultDuration = hours.toLong().hours
    }
}
