@file:OptIn(ExperimentalMaterial3Api::class)

package com.chris.culsi
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.chris.culsi.alerts.CulsiNotifications
import com.chris.culsi.catalog.CatalogCategory
import com.chris.culsi.catalog.ItemCatalogViewModel
import com.chris.culsi.data.CulsiDb
import com.chris.culsi.data.DiscardAction
import com.chris.culsi.data.ExportFormat
import com.chris.culsi.data.FoodLog
import com.chris.culsi.print.LabelTemplate
import com.chris.culsi.print.LabelTemplates
import com.chris.culsi.ui.FoodLogViewModel
import com.chris.culsi.ui.LabelBatchViewModel
import com.chris.culsi.ui.theme.CulsiTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    private val vm: FoodLogViewModel by viewModels()
    private val batchVm: LabelBatchViewModel by viewModels()
    private val itemCatalogVM: ItemCatalogViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CulsiNotifications.ensureChannels(this)
        maybeRequestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            CulsiTheme {
                var showAudit by remember { mutableStateOf(false) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Culsi — Time as Public Health Control") },
                            actions = {
                                TextButton(onClick = { showAudit = !showAudit }) {
                                    Text(if (showAudit) "Logs" else "Audit")
                                }
                            }
                        )
                    }
                ) { inner ->
                    if (showAudit) {
                        AuditScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(inner),
                            vm = vm
                        )
                    } else {
                        MainScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(inner),
                            vm = vm,
                            batchVm = batchVm,
                            catalogVm = itemCatalogVM,
                            onSaveCustomItem = { itemCatalogVM.addCustom(it) }
                        )
                    }
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = getSharedPreferences("culsi_prefs", Context.MODE_PRIVATE)
        val requested = prefs.getBoolean("notif_permission_requested", false)
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted || requested) return

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            100
        )
        prefs.edit { putBoolean("notif_permission_requested", true) }
    }
}

private data class NewLogForm(
    val item: String = "",
    val hours: Int = 4,
    val who: String = "",
    val notes: String = "",
    val useCustom: Boolean = false,
    val customLeftAt: Long? = null,
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MainScreen(
    modifier: Modifier = Modifier,
    vm: FoodLogViewModel,
    batchVm: LabelBatchViewModel,
    catalogVm: ItemCatalogViewModel,
    onSaveCustomItem: (String) -> Unit,
) {
    val active by vm.active.collectAsState()
    val recent by vm.recent.collectAsState()
    val all by vm.all.collectAsState()
    val printConfig by batchVm.printConfig.collectAsState()
    var showAllHistory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember(context) { CulsiDb.get(context) }
    val averyRepo = remember(db) { AverySheetRepository(db.averySheetStateDao()) }
    val batchCount by batchVm.batchCount.collectAsState()
    val hasLastBatch by batchVm.hasLastBatch.collectAsState()
    var sheetVersion by remember { mutableIntStateOf(0) }
    val averyState by produceState<AverySheetState?>(initialValue = null, printConfig.templateId, printConfig.templateType, sheetVersion) {
        value = if (printConfig.templateType == TemplateType.AVERY_SHEET && printConfig.avery != null) {
            averyRepo.getOrInit(printConfig)
        } else {
            null
        }
    }
    var editableMask by remember { mutableStateOf("") }
    LaunchedEffect(averyState?.usedMask) {
        editableMask = averyState?.usedMask ?: ""
    }
    var usePartialSheet by rememberSaveable { mutableStateOf(false) }
    var startRowText by rememberSaveable { mutableStateOf("1") }
    var startColText by rememberSaveable { mutableStateOf("1") }
    LaunchedEffect(printConfig.templateType) {
        if (printConfig.templateType != TemplateType.AVERY_SHEET) {
            usePartialSheet = false
        }
    }
    LaunchedEffect(printConfig.templateId) {
        startRowText = "1"
        startColText = "1"
    }
    var showInsufficientCellsDialog by remember { mutableStateOf(false) }
    var pendingPrintRequest by remember { mutableStateOf<PendingPrintRequest?>(null) }
    var insufficientSpaceInfo by remember { mutableStateOf<GridPlanResult.InsufficientSpace?>(null) }
    var canUndo by remember { mutableStateOf(LabelPrinter.canUndoLastPrint()) }
    val metrics = printConfig.avery
    val rows = metrics?.rows ?: 0
    val cols = metrics?.cols ?: 0
    val totalCells = rows * cols
    LaunchedEffect(rows, cols) {
        val maxRow = rows.coerceAtLeast(1)
        val maxCol = cols.coerceAtLeast(1)
        val normalizedRow = startRowText.toIntOrNull()?.coerceIn(1, maxRow) ?: 1
        val normalizedCol = startColText.toIntOrNull()?.coerceIn(1, maxCol) ?: 1
        startRowText = normalizedRow.toString()
        startColText = normalizedCol.toString()
    }
    val startRow = startRowText.toIntOrNull()?.coerceIn(1, rows.coerceAtLeast(1)) ?: 1
    val startCol = startColText.toIntOrNull()?.coerceIn(1, cols.coerceAtLeast(1)) ?: 1
    fun ensureMask(mask: String): String {
        val total = rows * cols
        if (total <= 0) return ""
        return when {
            mask.length == total -> mask
            mask.isEmpty() -> "0".repeat(total)
            mask.length > total -> mask.take(total)
            else -> mask.padEnd(total, '0')
        }
    }
    val previewPlan by remember(
        editableMask,
        rows,
        cols,
        usePartialSheet,
        startRow,
        startCol,
        batchCount,
        printConfig.templateType
    ) {
        derivedStateOf {
            if (printConfig.templateType != TemplateType.AVERY_SHEET || metrics == null) return@derivedStateOf null
            if (batchCount <= 0) return@derivedStateOf null
            val mask = ensureMask(editableMask)
            val startRowZero = if (usePartialSheet && rows > 0) (startRow - 1).coerceIn(0, rows - 1) else null
            val startColZero = if (usePartialSheet && cols > 0) (startCol - 1).coerceIn(0, cols - 1) else null
            when (
                val planResult = planAveryGrid(
                    rows = rows,
                    cols = cols,
                    usedMask = mask,
                    labelsNeeded = batchCount,
                    startRow = startRowZero,
                    startCol = startColZero
                )
            ) {
                is GridPlanResult.Success -> planResult.toPlacementPlan(rows, cols)
                is GridPlanResult.InsufficientSpace -> null
            }
        }
    }
    val previewNumbers by remember(previewPlan) {
        derivedStateOf {
            previewPlan?.pages
                ?.flatMap { it.cellIndices }
                ?.mapIndexed { index, cell -> cell.index to index + 1 }
                ?.toMap()
                ?: emptyMap()
        }
    }
    fun toggleCell(index: Int) {
        if (printConfig.templateType != TemplateType.AVERY_SHEET || metrics == null) return
        if (index !in 0 until totalCells) return
        val current = ensureMask(editableMask).toCharArray()
        current[index] = if (current[index] == '1') '0' else '1'
        val updated = current.concatToString()
        editableMask = updated
        scope.launch {
            averyRepo.save(
                AverySheetState(
                    templateId = printConfig.templateId,
                    rows = rows,
                    cols = cols,
                    usedMask = updated
                )
            )
            sheetVersion++
        }
    }
    suspend fun startNewSheet() {
        if (printConfig.templateType != TemplateType.AVERY_SHEET || metrics == null) return
        val blank = if (totalCells > 0) "0".repeat(totalCells) else ""
        averyRepo.clear(printConfig.templateId)
        val newState = AverySheetState(
            templateId = printConfig.templateId,
            rows = rows,
            cols = cols,
            usedMask = blank
        )
        averyRepo.save(newState)
        editableMask = blank
        sheetVersion++
    }
    suspend fun performPrint(labels: List<LabelData>, useBatchData: Boolean) {
        if (useBatchData.not() && labels.isEmpty()) return
        pendingPrintRequest = null
        insufficientSpaceInfo = null
        if (printConfig.templateType != TemplateType.AVERY_SHEET || metrics == null) {
            if (useBatchData) {
                batchVm.printBatch(context, placement = null) {
                    canUndo = LabelPrinter.canUndoLastPrint()
                }
            } else {
                LabelPrinter.printLabels(context, labels, printConfig, placement = null) {
                    canUndo = LabelPrinter.canUndoLastPrint()
                }
            }
            return
        }
        val actualLabels = if (useBatchData) {
            val payloads = batchVm.buildCurrentBatchList()
            if (payloads.isEmpty()) return
            payloads.map {
                LabelData(
                    itemName = it.itemName,
                    leftAt = it.leftAt.toEpochMilli(),
                    discardAt = it.dueAt.toEpochMilli(),
                    loggedBy = it.loggedBy,
                    notes = it.notes.ifBlank { null }
                )
            }
        } else {
            labels
        }
        if (actualLabels.isEmpty()) return
        val mask = ensureMask(editableMask)
        val startRowZero = if (usePartialSheet && rows > 0) (startRow - 1).coerceIn(0, rows - 1) else null
        val startColZero = if (usePartialSheet && cols > 0) (startCol - 1).coerceIn(0, cols - 1) else null
        when (
            val planResult = planAveryGrid(
                rows = rows,
                cols = cols,
                usedMask = mask,
                labelsNeeded = actualLabels.size,
                startRow = startRowZero,
                startCol = startColZero
            )
        ) {
            is GridPlanResult.InsufficientSpace -> {
                pendingPrintRequest = PendingPrintRequest(actualLabels, useBatchData)
                insufficientSpaceInfo = planResult
                showInsufficientCellsDialog = true
                return
            }

            is GridPlanResult.Success -> {
                insufficientSpaceInfo = null
                val plan = planResult.toPlacementPlan(rows, cols)
                val snapshot = averyState?.copy() ?: AverySheetState(
                    templateId = printConfig.templateId,
                    rows = rows,
                    cols = cols,
                    usedMask = mask
                )
                LabelPrinter.recordSheetSnapshotBeforePrint(snapshot)
                canUndo = LabelPrinter.canUndoLastPrint()
                val newMask = applyPlanToMask(mask, plan)
                val updatedState = AverySheetState(
                    templateId = printConfig.templateId,
                    rows = rows,
                    cols = cols,
                    usedMask = newMask
                )
                averyRepo.save(updatedState)
                editableMask = newMask
                sheetVersion++
                val onSpool: () -> Unit = {
                    canUndo = LabelPrinter.canUndoLastPrint()
                }
                if (useBatchData) {
                    batchVm.printBatch(context, placement = plan, onSpool = onSpool)
                } else {
                    LabelPrinter.printLabels(context, actualLabels, printConfig, placement = plan, onSpool = onSpool)
                }
                pendingPrintRequest = null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            // leave room so bottom items aren’t hidden behind the FAB
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Section: AddLogCard
            item {
                val catalogItems by catalogVm.items.collectAsState()
                val lastLoggedBy by catalogVm.lastLoggedBy.collectAsState()
                val currentCategory by catalogVm.currentCategory.collectAsState()
    
                // live snapshot for label area
                var form by remember { mutableStateOf(NewLogForm()) }
    
                AddLogCard(
                    currentCategory = currentCategory,
                    onSetCategory = catalogVm::setCategory,
                    itemOptions = catalogItems,
                    initialWho = lastLoggedBy,
                    onRememberLoggedBy = catalogVm::rememberLastLoggedBy,
                    onSaveCustomItem = onSaveCustomItem,
                    onAdd = { item, leftAt, hours, who, notes ->
                        vm.createLog(
                            itemName = item,
                            leftTempAt = leftAt,
                            durationMillis = (hours * 60L * 60L * 1000L),
                            loggedBy = who,
                            notes = notes
                        )
                    },
                    onFormChanged = { item, hours, who, notes, useCustom, customLeftAt ->
                        form = NewLogForm(
                            item = item,
                            hours = hours,
                            who = who,
                            notes = notes,
                            useCustom = useCustom,
                            customLeftAt = customLeftAt
                        )
                    }
                )
    
                // Label actions area (under the form)
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val now = System.currentTimeMillis()
                        val leftAtForPrint = if (form.useCustom) (form.customLeftAt ?: now) else now
                        val canPrintSingle = form.item.isNotBlank() && form.who.isNotBlank()

                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Print single label
                            Button(
                                enabled = canPrintSingle,
                                onClick = {
                                    val dueAt = leftAtForPrint + form.hours * 60L * 60L * 1000L
                                    val label = LabelData(
                                        itemName = form.item,
                                        leftAt = leftAtForPrint,
                                        discardAt = dueAt,
                                        loggedBy = form.who,
                                        notes = form.notes.ifBlank { null }
                                    )
                                    scope.launch {
                                        performPrint(listOf(label), useBatchData = false)
                                    }
                                }
                            ) { Text("Print Label") }

                            // Print current batch (no auto-clear)
                            Button(
                                onClick = {
                                    scope.launch {
                                        performPrint(emptyList(), useBatchData = true)
                                    }
                                },
                                enabled = batchCount > 0
                            ) { Text("Print Batch") }

                            // Clear current batch explicitly
                            OutlinedButton(
                                enabled = batchCount > 0,
                                onClick = { batchVm.clearBatch() }
                            ) { Text("Clear Batch") }

                            // Batch counter (will wrap if needed)
                            Text("Batch: $batchCount")
                        }
                    }
                }
            }
    
            // Section: label settings
            item {
                LabelSettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    config = printConfig,
                    templates = LabelTemplates.all,
                    onTemplateSelected = batchVm::setTemplate,
                    onScaleChanged = batchVm::setScalePct,
                    onXOffsetChanged = batchVm::setXOffsetPts,
                    onYOffsetChanged = batchVm::setYOffsetPts,
                    hasCachedBatch = hasLastBatch,
                    onReprintIdentical = { batchVm.reprintLastBatch(context) },
                    canUndo = canUndo,
                    onUndoLastPrint = {
                        scope.launch {
                            LabelPrinter.undoLastPrint(averyRepo)
                            val refreshed = if (printConfig.templateType == TemplateType.AVERY_SHEET && printConfig.avery != null) {
                                averyRepo.getOrInit(printConfig)
                            } else {
                                null
                            }
                            editableMask = refreshed?.usedMask ?: ""
                            sheetVersion++
                            canUndo = LabelPrinter.canUndoLastPrint()
                        }
                    },
                    partialSheetContent = {
                        if (printConfig.templateType == TemplateType.AVERY_SHEET && metrics != null) {
                            PartialSheetControls(
                                usePartialSheet = usePartialSheet,
                                onUsePartialSheetChanged = { usePartialSheet = it },
                                startRowText = startRowText,
                                onStartRowChange = { startRowText = it.filter { ch -> ch.isDigit() }.take(2) },
                                startColText = startColText,
                                onStartColChange = { startColText = it.filter { ch -> ch.isDigit() }.take(2) },
                                onStartNewSheet = {
                                    scope.launch { startNewSheet() }
                                },
                                onResetCells = {
                                    scope.launch { startNewSheet() }
                                },
                                rows = rows,
                                cols = cols,
                                editableMask = ensureMask(editableMask),
                                onToggleCell = ::toggleCell,
                                placementPreview = previewNumbers,
                                labelsNeeded = batchCount
                            )
                        }
                    }
                )
            }
    
            // Section: active items header
            item {
                Text("Active Items", style = MaterialTheme.typography.titleMedium)
                if (active.isEmpty()) {
                    Text("No active items.")
                }
            }
    
            // Section: active items list
            items(
                items = active,
                key = { log -> "active-${log.id}" }
            ) { log ->
                val qty = batchVm.countFor(log.id)
                val inBatch = qty > 0
                val labelData = LabelData(
                    itemName = log.itemName,
                    leftAt = log.leftTempAt,
                    discardAt = log.discardDueAt,
                    loggedBy = log.loggedBy,
                    notes = log.notes?.takeIf { it.isNotBlank() }
                )
    
                ActiveLogRow(
                    itemName = log.itemName,
                    leftAt = log.leftTempAt,
                    dueAt = log.discardDueAt,
                    loggedBy = log.loggedBy,
                    // notes is now nullable in ActiveLogRow
                    inBatch = inBatch,
                    qtyInBatch = qty,
                    onPrintLabel = {
                        scope.launch { performPrint(listOf(labelData), useBatchData = false) }
                    },
                    onAddToBatch = {
                        batchVm.addOrIncrement(
                            log.id,
                            LabelBatchViewModel.LabelPayload(
                                itemName = log.itemName,
                                leftAt = Instant.ofEpochMilli(log.leftTempAt),
                                dueAt = Instant.ofEpochMilli(log.discardDueAt),
                                loggedBy = log.loggedBy,
                                notes = log.notes.orEmpty(),
                                config = batchVm.currentConfig
                            )
                        )
                    },
                    onInc = { batchVm.increment(log.id) },
                    onDec = { batchVm.decrement(log.id) },
                    onRemoveFromBatch = { batchVm.removeAllForLog(log.id) },
                    onDiscard = { action ->
                        batchVm.removeAllForLog(log.id)
                        vm.discard(log.id, action)
                    },
                    onDelete = {
                        batchVm.removeAllForLog(log.id)
                        vm.delete(log.id)
                    }
                )
            }
    
            // Section: history header + toggle
            item {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (showAllHistory) "History (all)" else "History (last 30 days)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { showAllHistory = !showAllHistory }) {
                        Text(if (showAllHistory) "Recent logs" else "All logs")
                    }
                }
            }
    
            // Section: history list
            val activeIds = active.map { it.id }.toSet()
            val history = (if (showAllHistory) all else recent).filter { it.id !in activeIds }
    
            items(
                items = history,
                key = { log -> "history-${log.id}" }
            ) { log ->
                HistoryRow(
                    title = log.itemName,
                    subtitle = "Left: ${fmtDateTime(log.leftTempAt)}  •  Due: ${fmtDateTime(log.discardDueAt)}",
                    trailing = when {
                        log.discardedAt == null -> "ACTIVE"
                        else -> "Discarded ${fmtDateTime(log.discardedAt)} (${log.discardAction})"
                    }
                )
            }
        }

        // Floating "Print Batch (N)" button
        AnimatedVisibility(
            visible = batchCount > 0,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        // Reuse your existing print path that handles Avery planning, masks, dialogs, etc.
                        performPrint(emptyList(), useBatchData = true)
                    }
                },
                icon = { Icon(Icons.Filled.Print, contentDescription = "Print batch") },
                text = { Text("Print Batch ($batchCount)") }
            )
        }
    }

    if (showInsufficientCellsDialog) {
        val info = insufficientSpaceInfo
        AlertDialog(
            onDismissRequest = {
                showInsufficientCellsDialog = false
                pendingPrintRequest = null
                insufficientSpaceInfo = null
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val pending = pendingPrintRequest
                        pendingPrintRequest = null
                        showInsufficientCellsDialog = false
                        insufficientSpaceInfo = null
                        startNewSheet()
                        if (pending != null) {
                            performPrint(pending.labels, pending.useBatchData)
                        }
                    }
                }) {
                    Text("Start new sheet")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingPrintRequest = null
                    insufficientSpaceInfo = null
                    showInsufficientCellsDialog = false
                }) {
                    Text("Cancel")
                }
            },
            title = { Text("Not enough free cells") },
            text = {
                val message = info?.let {
                    "Not enough labels left on this sheet after the selected start cell. " +
                        "Requested ${it.requested}, available ${it.available}."
                } ?: "Not enough free cells remain on this sheet. Start a new sheet and continue?"
                Text(message)
            }
        )
    }
}

@Composable
private fun LabelSettingsCard(
    modifier: Modifier = Modifier,
    config: LabelPrintConfig,
    templates: List<LabelTemplate>,
    onTemplateSelected: (LabelTemplate) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onXOffsetChanged: (Float) -> Unit,
    onYOffsetChanged: (Float) -> Unit,
    hasCachedBatch: Boolean,
    onReprintIdentical: () -> Unit,
    canUndo: Boolean,
    onUndoLastPrint: () -> Unit,
    partialSheetContent: @Composable ColumnScope.() -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Label printing",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (expanded) "▾" else "▸",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val resolvedTemplate = remember(config.templateId) {
                LabelTemplates.findById(config.templateId) ?: LabelTemplates.default
            }

            if (!expanded) {
                val templateName = resolvedTemplate.name
                val paperWidthInches = resolvedTemplate.pageWidthPts / 72f
                val paperHeightInches = resolvedTemplate.pageHeightPts / 72f
                val paperSize = String.format(Locale.US, "%.2f×%.2f in", paperWidthInches, paperHeightInches)
                val scaleTextSummary = formatFloatInput(config.scalePct)
                val xOffsetSummary = formatFloatInput(config.xOffsetPts)
                val yOffsetSummary = formatFloatInput(config.yOffsetPts)
                Text(
                    "Current: $templateName • $paperSize • scale $scaleTextSummary% • x $xOffsetSummary / y $yOffsetSummary", // Corrected string template
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Template", style = MaterialTheme.typography.titleSmall)
                        templates.forEach { template ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = template.id == config.templateId,
                                    onClick = { onTemplateSelected(template) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(template.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Alignment tweaks", style = MaterialTheme.typography.titleSmall)
                        Text("Offsets are in PDF points (1 pt = 1/72\"). Positive values move right/down.")
                        var xOffsetText by remember { mutableStateOf(formatFloatInput(config.xOffsetPts)) }
                        var yOffsetText by remember { mutableStateOf(formatFloatInput(config.yOffsetPts)) }
                        var scaleText by remember { mutableStateOf(formatFloatInput(config.scalePct)) }
                        LaunchedEffect(config.xOffsetPts) {
                            xOffsetText = formatFloatInput(config.xOffsetPts)
                        }
                        LaunchedEffect(config.yOffsetPts) {
                            yOffsetText = formatFloatInput(config.yOffsetPts)
                        }
                        LaunchedEffect(config.scalePct) { scaleText = formatFloatInput(config.scalePct) }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = xOffsetText,
                                onValueChange = {
                                    xOffsetText = it
                                    it.toFloatOrNull()?.let(onXOffsetChanged)
                                },
                                label = { Text("X offset (pts)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(120.dp)
                            )
                            OutlinedTextField(
                                value = yOffsetText,
                                onValueChange = {
                                    yOffsetText = it
                                    it.toFloatOrNull()?.let(onYOffsetChanged)
                                },
                                label = { Text("Y offset (pts)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(120.dp)
                            )
                            OutlinedTextField(
                                value = scaleText,
                                onValueChange = {
                                    scaleText = it
                                    it.toFloatOrNull()?.takeIf { value -> value > 0f }?.let(onScaleChanged)
                                },
                                label = { Text("Scale (%)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(120.dp)
                            )
                        }
                        Text("Use scale to adjust drift (e.g., 99 = -1%).")
                    }

                    partialSheetContent()
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onReprintIdentical, enabled = hasCachedBatch) {
                    Text("Reprint identical")
                }
                OutlinedButton(onClick = onUndoLastPrint, enabled = canUndo) {
                    Text("Undo last print")
                }
            }
        }
    }
}

@Composable
private fun PartialSheetControls(
    usePartialSheet: Boolean,
    onUsePartialSheetChanged: (Boolean) -> Unit,
    startRowText: String,
    onStartRowChange: (String) -> Unit,
    startColText: String,
    onStartColChange: (String) -> Unit,
    onStartNewSheet: () -> Unit,
    onResetCells: () -> Unit,
    rows: Int,
    cols: Int,
    editableMask: String,
    onToggleCell: (Int) -> Unit,
    placementPreview: Map<Int, Int>,
    labelsNeeded: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use partial Avery sheet", style = MaterialTheme.typography.titleSmall)
            Switch(checked = usePartialSheet, onCheckedChange = onUsePartialSheetChanged)
        }

        if (!usePartialSheet) {
            OutlinedButton(onClick = onStartNewSheet) { Text("Start new sheet") }
            return
        }

        if (rows <= 0 || cols <= 0) {
            Text("Template layout unavailable.", style = MaterialTheme.typography.bodyMedium)
            return
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = startRowText,
                onValueChange = onStartRowChange,
                label = { Text("Start row") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp)
            )
            OutlinedTextField(
                value = startColText,
                onValueChange = onStartColChange,
                label = { Text("Start column") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onStartNewSheet) { Text("Start new sheet") }
            OutlinedButton(onClick = onResetCells) { Text("Reset all cells") }
        }

        Text(
            "Batch labels: $labelsNeeded • Tap cells to toggle used spots.",
            style = MaterialTheme.typography.bodySmall
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(rows * cols) { index ->
                val isUsed = editableMask.getOrNull(index) == '1'
                val previewNumber = placementPreview[index]
                val shape = RoundedCornerShape(6.dp)
                val backgroundColor = if (isUsed) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(shape)
                        .background(backgroundColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                        .clickable { onToggleCell(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (previewNumber != null && previewNumber > 0) {
                        Text(
                            text = previewNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private data class PendingPrintRequest(
    val labels: List<LabelData>,
    val useBatchData: Boolean
)

@Composable
private fun AuditScreen(modifier: Modifier = Modifier, vm: FoodLogViewModel) {
    val recent by vm.recent.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier.padding(16.dp)) {
        var showConfirm by remember { mutableStateOf(false) }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                scope.launch {
                    val end = System.currentTimeMillis()
                    val start = end - TimeUnit.DAYS.toMillis(30)
                    val data = vm.exportLogs(start, end, ExportFormat.CSV)
                    val file = File(context.cacheDir, "food_logs.csv")
                    file.writeText(data)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Share logs"))
                }
            }) {
                Text("Export")
            }

            Button(onClick = { showConfirm = true }) {
                Text("Clear Log")
            }
        }

        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirm = false
                        scope.launch {
                            val end = System.currentTimeMillis()
                            val start = end - TimeUnit.DAYS.toMillis(30)
                            vm.clearLogs(start, end)
                        }
                    }) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Clear logs?") },
                text = { Text("This action cannot be undone. Export the log first if needed.") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recent, key = { it.id }) { log ->
                AuditRow(log)
            }
        }
    }
}

@Composable
private fun AuditRow(log: FoodLog) {
    ListItem(
        headlineContent = { Text(log.itemName) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Left: ${fmtDateTime(log.leftTempAt)}  •  Due: ${fmtDateTime(log.discardDueAt)}")
                Text(
                    when {
                        log.discardedAt != null -> "Discarded: ${fmtDateTime(log.discardedAt)} (${log.discardAction})"
                        else -> "Discard not recorded"
                    }
                )
                log.notes?.let { Text("Notes: $it") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLogCard(
    currentCategory: CatalogCategory,
    onSetCategory: (CatalogCategory) -> Unit,
    itemOptions: List<String>,
    initialWho: String,
    onRememberLoggedBy: (String) -> Unit,
    onSaveCustomItem: (String) -> Unit,
    onAdd: (item: String, leftAtMillis: Long, hours: Int, who: String, notes: String?) -> Unit,
    onFormChanged: (
        item: String,
        hours: Int,
        who: String,
        notes: String,
        useCustom: Boolean,
        customLeftAt: Long?
    ) -> Unit = { _, _, _, _, _, _ -> },
) {
    var item by remember { mutableStateOf("") }
    var itemExpanded by remember { mutableStateOf(false) }

    var who by remember { mutableStateOf(initialWho) }
    var whoExpanded by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf(listOf<String>()) }

    var hours by remember { mutableIntStateOf(4) }
    var hoursText by remember { mutableStateOf("4") }
    var notes by remember { mutableStateOf("") }

    var useCustom by remember { mutableStateOf(false) }
    var customDate by remember { mutableStateOf("") } // yyyy-MM-dd
    var customTime by remember { mutableStateOf("") } // HH:mm

    val customLeftAt: Long? by remember(customDate, customTime) {
        mutableStateOf(parseLocalDateTime(customDate, customTime))
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        val set = prefs.getStringSet("kitchen_members", emptySet()) ?: emptySet()
        members = set.toList().distinct().sortedBy { it.lowercase(Locale.getDefault()) }
    }
    LaunchedEffect(initialWho) {
        if (initialWho.isNotBlank()) {
            who = initialWho
            onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
        }
    }

    LaunchedEffect(customLeftAt) {
        onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
    }

    fun saveMembers(updated: List<String>) {
        members = updated
        prefs.edit {putStringSet("kitchen_members", updated.toSet())}
    }
    fun addMember(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val updated = (members + trimmed)
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedBy { it.lowercase(Locale.getDefault()) }
        saveMembers(updated)
    }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New TPHC Log", style = MaterialTheme.typography.titleMedium)

            // ---- Category chips moved here (single source of truth) ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { onSetCategory(CatalogCategory.BREAKFAST) },
                    label = { Text("Breakfast") },
                    enabled = currentCategory != CatalogCategory.BREAKFAST
                )
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = { onSetCategory(CatalogCategory.LUNCH_DINNER) },
                    label = { Text("Lunch & Dinner") },
                    enabled = currentCategory != CatalogCategory.LUNCH_DINNER
                )
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = { onSetCategory(CatalogCategory.SAUCES_MISE) },
                    label = { Text("Sauces & Mise") },
                    enabled = currentCategory != CatalogCategory.SAUCES_MISE
                )
            }

            // ---- Single editable dropdown for Food item (only one field now) ----
            ExposedDropdownMenuBox(expanded = itemExpanded, onExpandedChange = { itemExpanded = !itemExpanded }) {
                OutlinedTextField(
                    value = item,
                    onValueChange = {
                        item = it
                        onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                    },
                    label = { Text("Food item") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                    // For your editable TextField anchors (both “Food item” and “Logged by”):
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    singleLine = true
                )
                val filtered = if (item.isBlank()) itemOptions else itemOptions.filter { it.contains(item, ignoreCase = true) }
                ExposedDropdownMenu(expanded = itemExpanded, onDismissRequest = { itemExpanded = false }) {
                    filtered.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                item = option
                                itemExpanded = false
                                onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = item.trim().isNotEmpty(),
                    onClick = { item.trim().takeIf { it.isNotEmpty() }?.let(onSaveCustomItem) }
                ) { Text("Save to Catalog") }
                Text("Alphabetical; custom items persist.", style = MaterialTheme.typography.bodySmall)
            }

            // ---- Logged by (editable dropdown + savable) ----
            ExposedDropdownMenuBox(expanded = whoExpanded, onExpandedChange = { whoExpanded = !whoExpanded }) {
                OutlinedTextField(
                    value = who,
                    onValueChange = {
                        who = it
                        onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                    },
                    label = { Text("Logged by") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = whoExpanded) },
                    // For your editable TextField anchors (both “Food item” and “Logged by”):
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    singleLine = true
                )
                val filtered = if (who.isBlank()) members else members.filter { it.contains(who, ignoreCase = true) }
                ExposedDropdownMenu(expanded = whoExpanded, onDismissRequest = { whoExpanded = false }) {
                    filtered.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                who = name
                                whoExpanded = false
                                onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                            }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = who.trim().isNotEmpty(),
                    onClick = {
                        val v = who.trim()
                        if (v.isNotEmpty()) {
                            addMember(v)
                            onRememberLoggedBy(v)
                        }
                    }
                ) { Text("Save Member") }
                Text("Members stored locally and sorted A–Z.", style = MaterialTheme.typography.bodySmall)
            }

            // ---- Hours + TPHC note ----
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = {
                        hoursText = it
                        it.toIntOrNull()?.let { h ->
                            if (h in 1..6) {
                                hours = h
                                onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                            }
                        }
                    },
                    label = { Text("Hold hours (1–6)") },
                    singleLine = true,
                    modifier = Modifier.width(160.dp)
                )
                Text("Default is 4.", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(
                    checked = useCustom,
                    onCheckedChange = {
                        useCustom = it
                        onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                    }
                )
                Text("Create custom time log & label")
            }

            if (useCustom) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customDate,
                            onValueChange = {
                                customDate = it
                                onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                            },
                            label = { Text("Left date (yyyy-MM-dd)") },
                            singleLine = true,
                            modifier = Modifier.width(200.dp)
                        )
                        OutlinedTextField(
                            value = customTime,
                            onValueChange = {
                                customTime = it
                                onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                            },
                            label = { Text("Left time (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.width(160.dp)
                        )
                    }
                    val previewLeft = customLeftAt?.let { fmtDateTime(it) } ?: "—"
                    val previewDue = customLeftAt?.let { fmtDateTime(it + hours * 60L * 60L * 1000L) } ?: "—"
                    Text("Preview • Left: $previewLeft  •  Due: $previewDue", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                "TPHC allows up to 4 hours for hot items and up to 6 hours for cold. Always check with your local health department.",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                    onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = item.isNotBlank() && who.isNotBlank(),
                onClick = {
                    val trimmedItem = item.trim()
                    val trimmedWho = who.trim()
                    val holdHours = hoursText.toIntOrNull()?.coerceIn(1, 6) ?: hours.coerceIn(1, 6)
                    val leftAtMillis = if (useCustom) (customLeftAt ?: System.currentTimeMillis()) else System.currentTimeMillis()
                    onAdd(trimmedItem, leftAtMillis, holdHours, trimmedWho, notes.trim().ifBlank { null })
                    onRememberLoggedBy(trimmedWho)
                    addMember(trimmedWho)
                    // reset relevant fields
                    item = ""
                    itemExpanded = false
                    hours = 4
                    hoursText = "4"
                    notes = ""
                    onFormChanged(item, hours, who, notes, useCustom, customLeftAt)
                }
            ) { Text("Add Log") }
        }
    }
}

@Composable
private fun ActiveLogRow(
    itemName: String,
    leftAt: Long,
    dueAt: Long,
    loggedBy: String,
    // Changed to nullable
    inBatch: Boolean,
    qtyInBatch: Int,
    onPrintLabel: () -> Unit,
    onAddToBatch: () -> Unit,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRemoveFromBatch: () -> Unit,
    onDiscard: (DiscardAction) -> Unit,
    onDelete: () -> Unit,
) {
    val remaining by rememberCountdown(dueAt)
    val overdue = remaining <= 0L

    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(itemName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Left temp: ${fmtDateTime(leftAt)}  •  Due: ${fmtDateTime(dueAt)}")
            Text(
                if (overdue) "OVERDUE — discard now" else "Time remaining: ${fmtDuration(remaining)}",
                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text("Logged by: $loggedBy")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onPrintLabel()
                }) {
                    Text("Print Label")
                }

                if (!inBatch) {
                    Button(onClick = onAddToBatch) {
                        Text("Add to Print Batch")
                    }
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("In batch ×$qtyInBatch") },
                        leadingIcon = {
                            Icon(Icons.Filled.Check, contentDescription = "In batch")
                        }
                    )
                    OutlinedIconButton(onClick = onInc) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase")
                    }
                    OutlinedIconButton(onClick = onDec, enabled = qtyInBatch > 1) {
                        Text("−")
                    }
                    IconButton(onClick = onRemoveFromBatch) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove from batch")
                    }
                }

                DiscardMenu(onDiscard = onDiscard)

                Button(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun DiscardMenu(onDiscard: (DiscardAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { open = true }) { Text("Record Discard") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                DiscardAction.TRASH,
                DiscardAction.COMPOST,
                DiscardAction.DONATION,
                DiscardAction.OTHER
            ).forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onDiscard(action)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(title: String, subtitle: String, trailing: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Text(trailing) }
    )
}

@Composable
private fun rememberCountdown(targetMillis: Long): State<Long> {
    val state = remember { mutableLongStateOf(targetMillis - System.currentTimeMillis()) }
    LaunchedEffect(targetMillis) {
        while (true) {
            state.longValue = targetMillis - System.currentTimeMillis()
            delay(1000)
        }
    }
    return state
}

private fun formatFloatInput(value: Float): String {
    val intValue = value.toInt()
    return if (value == intValue.toFloat()) {
        intValue.toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

private fun parseLocalDateTime(date: String, time: String): Long? {
    if (date.isBlank() || time.isBlank()) return null
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        fmt.parse("$date $time")?.time
    } catch (_: Exception) {
        null
    }
}

private fun fmtDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun fmtDuration(remainingMillis: Long): String {
    val total = remainingMillis.coerceAtLeast(0L)
    val hours = total / 3_600_000
    val mins = (total % 3_600_000) / 60_000
    val secs = (total % 60_000) / 1000
    return "%02d:%02d:%02d".format(hours, mins, secs)
}

@Preview
@Composable
private fun PreviewRow() {
    CulsiTheme {
        ActiveLogRow(
            itemName = "Grilled Chicken",
            leftAt = System.currentTimeMillis() - 10 * 60 * 1000,
            dueAt = System.currentTimeMillis() + 3_600_000,
            loggedBy = "Chef Chris",
            // notes is now nullable
            inBatch = false, // Added dummy inBatch
            qtyInBatch = 0, // Added dummy qtyInBatch
            onPrintLabel = {},
            onAddToBatch = {}, // Corrected dummy onAddToBatch
            onInc = {}, // Added dummy onInc
            onDec = {}, // Added dummy onDec
            onRemoveFromBatch = {}, // Added dummy onRemoveFromBatch
            onDiscard = {},
            onDelete = {}
        )
    }
}
