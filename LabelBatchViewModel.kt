package com.chris.culsi.ui

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chris.culsi.LabelData
import com.chris.culsi.LabelPrintConfig
import com.chris.culsi.LabelPrinter
import com.chris.culsi.PlacementPlan
import com.chris.culsi.print.LabelTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class LabelBatchViewModel : ViewModel() {

    data class LabelPayload(
        val itemName: String,
        val leftAt: Instant,
        val dueAt: Instant,
        val loggedBy: String,
        val notes: String,
        val config: LabelPrintConfig
    )

    private val _quantities = mutableStateMapOf<Long, Int>()

    private val _payloads = mutableMapOf<Long, LabelPayload>()

    private var lastBatch: List<Pair<Long, LabelPayload>> = emptyList()

    // Expose state to UI
    private val _batchCount = MutableStateFlow(0)
    val batchCount: StateFlow<Int> = _batchCount.asStateFlow()

    private val _hasLastBatch = MutableStateFlow(LabelPrinter.hasCachedBatch())
    val hasLastBatch: StateFlow<Boolean> = _hasLastBatch.asStateFlow()

    // Printing config (template/scale/offsets) already exists in your app:
    private val _printConfig = MutableStateFlow(LabelPrintConfig())
    val printConfig: StateFlow<LabelPrintConfig> = _printConfig.asStateFlow()
    val currentConfig: LabelPrintConfig get() = _printConfig.value

    private fun updateBatchCount() {
        _batchCount.value = _quantities.values.sum()
    }

    fun setTemplate(template: LabelTemplate) {
        _printConfig.update {
            it.copy(
                templateId = template.id,
                templateType = template.templateType,
                avery = template.averyMetrics
            )
        }
    }

    fun setScalePct(scalePct: Float) {
        _printConfig.update { it.copy(scalePct = scalePct) }
    }

    fun setXOffsetPts(offset: Float) {
        _printConfig.update { it.copy(xOffsetPts = offset) }
    }

    fun setYOffsetPts(offset: Float) {
        _printConfig.update { it.copy(yOffsetPts = offset) }
    }

    fun addOrIncrement(logId: Long, payload: LabelPayload) {
        _payloads.putIfAbsent(logId, payload)
        _quantities[logId] = (_quantities[logId] ?: 0) + 1
        updateBatchCount()
    }

    fun increment(logId: Long) {
        val cur = _quantities[logId] ?: return
        _quantities[logId] = cur + 1
        updateBatchCount()
    }

    fun decrement(logId: Long) {
        val cur = _quantities[logId] ?: return
        when {
            cur > 1 -> {
                _quantities[logId] = cur - 1
            }
            cur == 1 -> {
                _quantities.remove(logId)
                _payloads.remove(logId)
            }
        }
        updateBatchCount()
    }

    fun removeAllForLog(logId: Long) {
        _quantities.remove(logId)
        _payloads.remove(logId)
        updateBatchCount()
    }

    fun countFor(logId: Long): Int = _quantities[logId] ?: 0

    fun buildCurrentBatchList(): List<LabelPayload> =
        _quantities.flatMap { (logId, qty) ->
            val payload = _payloads[logId] ?: return@flatMap emptyList()
            List(qty) { payload }
        }

    fun clearAll() {
        _quantities.clear()
        _payloads.clear()
        updateBatchCount()
    }

    fun clearBatch() {
        clearAll()
        // Do NOT touch lastBatch or the cached PDF/config here (so reprint remains available).
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun printBatch(
        context: Context,
        placement: PlacementPlan? = null,
        onSpool: (() -> Unit)? = null
    ) {
        val list = buildCurrentBatchList()
        if (list.isEmpty()) return

        val data = list.map {
            LabelData(
                itemName = it.itemName,
                leftAt = it.leftAt.toEpochMilli(),
                discardAt = it.dueAt.toEpochMilli(),
                loggedBy = it.loggedBy,
                notes = it.notes.ifBlank { null }
            )
        }

        val config = currentConfig

        // Cache for reprint
        lastBatch = _quantities.flatMap { (logId, qty) ->
            val payload = _payloads[logId] ?: return@flatMap emptyList()
            List(qty) { logId to payload }
        }

        viewModelScope.launch {
            LabelPrinter.printLabels(
                context = context,
                items = data,
                config = config,
                placement = placement,
                onSpool = {
                    _hasLastBatch.value = LabelPrinter.hasCachedBatch()
                    onSpool?.invoke()
                }
            )
        }
    }

    fun reprintLastBatch(context: Context) {
        LabelPrinter.reprintIdentical(context)
        _hasLastBatch.value = LabelPrinter.hasCachedBatch()
    }

}
