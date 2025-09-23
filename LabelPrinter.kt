package com.chris.culsi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.chris.culsi.print.LabelTemplate
import com.chris.culsi.print.LabelTemplates
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.core.graphics.withSave

enum class TemplateType { THERMAL, AVERY_SHEET }

data class AveryTemplateMetrics(
    val pageWidthMm: Float,
    val pageHeightMm: Float,
    val rows: Int,
    val cols: Int,
    val labelWidthMm: Float,
    val labelHeightMm: Float,
    val marginTopMm: Float,
    val marginLeftMm: Float,
    val hPitchMm: Float,
    val vPitchMm: Float,
)

data class LabelPrintConfig(
    val templateId: String = LabelTemplates.default.id,
    val templateType: TemplateType = LabelTemplates.default.templateType,
    val scalePct: Float = 100f,
    val xOffsetPts: Float = 0f,
    val yOffsetPts: Float = 0f,
    val avery: AveryTemplateMetrics? = LabelTemplates.default.averyMetrics,
)

data class LabelData(
    val itemName: String,
    val leftAt: Long,
    val discardAt: Long,
    val loggedBy: String,
    val notes: String? = null,
)

data class CellPlacement(
    val row: Int,
    val col: Int,
    val page: Int,
)

sealed class GridPlanResult {
    data class Success(
        val placements: List<CellPlacement>,
        val remainingFreeCells: Int,
    ) : GridPlanResult()

    data class InsufficientSpace(
        val requested: Int,
        val available: Int,
    ) : GridPlanResult()
}

/**
 * Plans Avery grid placements in reading order without wrapping back to cells before the start.
 * Cells prior to the provided [startRow] and [startCol] (when specified) are intentionally treated
 * as unavailable; only cells at or after the start position are considered. The planner skips
 * over cells marked as used in [usedMask] and, when [respectStartCellNoWrap] is `true`, stops at
 * the end of the sheet instead of wrapping around to earlier indices.
 */
fun planAveryGrid(
    rows: Int,
    cols: Int,
    usedMask: String,
    labelsNeeded: Int,
    startRow: Int? = null,
    startCol: Int? = null,
    respectStartCellNoWrap: Boolean = true,
): GridPlanResult {
    val totalCells = rows * cols
    if (totalCells <= 0) {
        return if (labelsNeeded <= 0) {
            GridPlanResult.Success(emptyList(), remainingFreeCells = 0)
        } else {
            GridPlanResult.InsufficientSpace(requested = labelsNeeded, available = 0)
        }
    }

    require(usedMask.length == totalCells) {
        "usedMask length (${usedMask.length}) must equal rows * cols ($totalCells)"
    }

    val zeroBasedStartRow = startRow?.coerceIn(0, rows - 1)
    val zeroBasedStartCol = startCol?.coerceIn(0, cols - 1)
    val startIndex = if (zeroBasedStartRow != null && zeroBasedStartCol != null) {
        zeroBasedStartRow * cols + zeroBasedStartCol
    } else {
        0
    }

    val orderedIndices: List<Int> = if (respectStartCellNoWrap) {
        if (startIndex >= totalCells) emptyList() else (startIndex until totalCells).toList()
    } else {
        List(totalCells) { offset -> (startIndex + offset) % totalCells }
    }

    val usableIndices = orderedIndices.filter { index -> usedMask[index] != '1' }
    val available = usableIndices.size

    if (labelsNeeded > available) {
        return GridPlanResult.InsufficientSpace(requested = labelsNeeded, available = available)
    }

    if (labelsNeeded <= 0) {
        return GridPlanResult.Success(emptyList(), remainingFreeCells = available)
    }

    val selected = usableIndices.take(labelsNeeded)
    val placements = selected.map { index ->
        val row = index / cols
        val col = index % cols
        val page = if (totalCells == 0) 0 else index / totalCells
        CellPlacement(row = row, col = col, page = page)
    }

    val remaining = available - placements.size
    return GridPlanResult.Success(placements = placements, remainingFreeCells = remaining)
}

fun GridPlanResult.Success.toPlacementPlan(rows: Int, cols: Int): PlacementPlan {
    if (placements.isEmpty() || rows <= 0 || cols <= 0) {
        return PlacementPlan(emptyList())
    }
    val pages = mutableListOf<MutableList<CellIndex>>()
    var currentPage = -1
    placements.forEach { placement ->
        val targetIndex = placement.row * cols + placement.col
        val pageIndex = placement.page
        val bucket = if (pageIndex == currentPage && pages.isNotEmpty()) {
            pages.last()
        } else {
            currentPage = pageIndex
            mutableListOf<CellIndex>().also { pages.add(it) }
        }
        bucket.add(CellIndex(targetIndex))
    }
    val pagePlacements = pages.map { indices -> PagePlacement(indices.toList()) }
    return PlacementPlan(pagePlacements)
}

private data class CachedBatch(
    val pdf: ByteArray,
    val config: LabelPrintConfig,
    val placement: PlacementPlan?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedBatch

        if (!pdf.contentEquals(other.pdf)) return false
        if (config != other.config) return false
        if (placement != other.placement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pdf.contentHashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + (placement?.hashCode() ?: 0)
        return result
    }
}

object LabelPrinter {

    private var lastCachedBatch: CachedBatch? = null
    private var lastSheetStateBeforePrint: AverySheetState? = null

    fun printLabels(
        context: Context,
        items: List<LabelData>,
        config: LabelPrintConfig,
        placement: PlacementPlan? = null,
        onSpool: (() -> Unit)? = null,
    ) {
        if (items.isEmpty()) return
        val template = resolveTemplate(config)
        val attrs = when (config.templateType) {
            TemplateType.THERMAL -> buildThermalPrintAttributes(template)
            TemplateType.AVERY_SHEET -> buildAveryPrintAttributes(config)
        }
        val pdfBytes = when (config.templateType) {
            TemplateType.THERMAL -> renderThermalPdf(items, config, template)
            TemplateType.AVERY_SHEET -> {
                val plan = placement ?: throw IllegalArgumentException("Placement plan required for Avery sheet printing")
                renderAveryPdf(items, config, template, plan)
            }
        }
        lastCachedBatch = CachedBatch(pdf = pdfBytes, config = config, placement = placement)
        spoolPdf(context, pdfBytes, attrs, jobNameFor(items))
        onSpool?.invoke()
    }

    fun reprintIdentical(context: Context) {
        val cached = lastCachedBatch ?: return
        val attrs = when (cached.config.templateType) {
            TemplateType.THERMAL -> buildThermalPrintAttributes(resolveTemplate(cached.config))
            TemplateType.AVERY_SHEET -> buildAveryPrintAttributes(cached.config)
        }
        spoolPdf(context, cached.pdf, attrs, jobNameFor(emptyList()))
    }

    fun recordSheetSnapshotBeforePrint(state: AverySheetState?) {
        lastSheetStateBeforePrint = state?.copy()
    }

    suspend fun undoLastPrint(repo: AverySheetRepository) {
        val snapshot = lastSheetStateBeforePrint ?: return
        repo.save(snapshot)
        lastSheetStateBeforePrint = null
    }

    fun hasCachedBatch(): Boolean = lastCachedBatch != null

    fun canUndoLastPrint(): Boolean = lastSheetStateBeforePrint != null

    private fun resolveTemplate(config: LabelPrintConfig): LabelTemplate =
        LabelTemplates.findById(config.templateId) ?: LabelTemplates.default
}

private val dateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
}

private fun renderThermalPdf(
    labels: List<LabelData>,
    config: LabelPrintConfig,
    template: LabelTemplate,
): ByteArray {
    val doc = PdfDocument()
    val perPage = template.labelsPerPage
    val scale = (config.scalePct / 100f).coerceAtLeast(0.01f)
    var index = 0
    var pageNumber = 1
    while (index < labels.size) {
        val pageInfo = PdfDocument.PageInfo.Builder(template.pageWidthPts, template.pageHeightPts, pageNumber).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.save()
        if (scale != 1f) {
            canvas.scale(scale, scale)
        }
        if (config.xOffsetPts != 0f || config.yOffsetPts != 0f) {
            canvas.translate(config.xOffsetPts / scale, config.yOffsetPts / scale)
        }
        val end = minOf(labels.size, index + perPage)
        for (i in index until end) {
            val offset = i - index
            val row = offset / template.cols
            val col = offset % template.cols
            val left = template.pageLeftMarginPts + col * (template.labelWidthPts + template.colGutterPts)
            val top = template.pageTopMarginPts + row * (template.labelHeightPts + template.rowGutterPts)
            drawLabel(canvas, labels[i], template, left.toFloat(), top.toFloat())
        }
        canvas.restore()
        doc.finishPage(page)
        index = end
        pageNumber++
    }
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
}

private fun renderAveryPdf(
    labels: List<LabelData>,
    config: LabelPrintConfig,
    template: LabelTemplate,
    placement: PlacementPlan,
): ByteArray {
    val metrics = config.avery ?: throw IllegalStateException("Avery metrics required")
    val doc = PdfDocument()
    val pageWidthPts = mmToPoints(metrics.pageWidthMm)
    val pageHeightPts = mmToPoints(metrics.pageHeightMm)
    val scale = (config.scalePct / 100f).coerceAtLeast(0.01f)
    var index = 0
    var pageNumber = 1
    placement.pages.forEach { pagePlacement ->
        if (index >= labels.size) return@forEach
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPts, pageHeightPts, pageNumber).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.withSave {
            if (scale != 1f) {
                scale(scale, scale)
            }
            if (config.xOffsetPts != 0f || config.yOffsetPts != 0f) {
                translate(config.xOffsetPts / scale, config.yOffsetPts / scale)
            }
            pagePlacement.cellIndices.forEach { cell ->
                if (index >= labels.size) return@forEach
                val cellRow = cell.index / metrics.cols
                val cellCol = cell.index % metrics.cols
                val leftMm = metrics.marginLeftMm + cellCol * metrics.hPitchMm
                val topMm = metrics.marginTopMm + cellRow * metrics.vPitchMm
                val left = mmToPoints(leftMm).toFloat()
                val top = mmToPoints(topMm).toFloat()
                drawLabel(this, labels[index], template, left, top)
                index++
            }
        }
        doc.finishPage(page)
        pageNumber++
    }
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
}

private fun drawLabel(
    canvas: Canvas,
    data: LabelData,
    template: LabelTemplate,
    left: Float,
    top: Float,
) {
    val padding = template.contentPaddingPts
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = template.defaultFontTitlePts
        isFakeBoldText = true
        color = Color.BLACK
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = template.defaultFontBodyPts
        color = Color.BLACK
    }
    val contentWidth = template.labelWidthPts.toFloat() - padding * 2
    val maxBaseline = top + template.labelHeightPts.toFloat() - padding
    var baseline = top + padding + titlePaint.textSize
    baseline = drawWrappedText(
        canvas,
        data.itemName,
        left + padding,
        baseline,
        contentWidth,
        titlePaint,
        template.titleLineSpacingPts,
        maxBaseline
    )
    baseline += template.bodyLineSpacingPts * 0.5f
    val bodyLines = listOf(
        "Left: ${formatDate(data.leftAt)}",
        "Discard: ${formatDate(data.discardAt)}",
        "By: ${data.loggedBy}"
    )
    for (line in bodyLines) {
        if (line.isBlank() || baseline > maxBaseline) break
        canvas.drawText(line, left + padding, baseline, bodyPaint)
        baseline += template.bodyLineSpacingPts
    }
    val notes = data.notes?.takeIf { it.isNotBlank() }
    if (notes != null && baseline <= maxBaseline) {
        drawWrappedText(
            canvas,
            "Notes: $notes",
            left + padding,
            baseline,
            contentWidth,
            bodyPaint,
            template.bodyLineSpacingPts,
            maxBaseline
        )
    }
}

private fun drawWrappedText(
    canvas: Canvas,
    text: String,
    x: Float,
    startBaseline: Float,
    maxWidth: Float,
    paint: Paint,
    lineSpacing: Float,
    maxBaseline: Float,
): Float {
    if (text.isBlank()) return startBaseline
    var baseline = startBaseline
    var index = 0
    val length = text.length
    while (index < length && baseline <= maxBaseline) {
        if (text[index] == '\n') {
            index++
            continue
        }
        val count = paint.breakText(text, index, length, true, maxWidth, null)
        if (count <= 0) break
        var end = index + count
        var forcedBreak = false
        val newlineIndex = text.indexOf('\n', index)
        if (newlineIndex in (index + 1)..end) {
            end = newlineIndex
            forcedBreak = true
        } else if (end < length) {
            val lastSpace = text.lastIndexOf(' ', end - 1)
            if (lastSpace >= index) {
                end = lastSpace
            }
        }
        if (end <= index) {
            end = (index + count).coerceAtMost(length)
        }
        val line = text.substring(index, end).trimEnd()
        if (line.isNotEmpty() && baseline <= maxBaseline) {
            canvas.drawText(line, x, baseline, paint)
            baseline += lineSpacing
        }
        index = if (forcedBreak) {
            end + 1
        } else {
            var next = end
            while (next < length && text[next] == ' ') {
                next++
            }
            next
        }
    }
    return baseline
}

private fun formatDate(epochMillis: Long): String = dateFormatter.get()!!.format(Date(epochMillis))

private fun buildThermalPrintAttributes(template: LabelTemplate): PrintAttributes {
    val widthMils = pointsToMils(template.pageWidthPts)
    val heightMils = pointsToMils(template.pageHeightPts)
    val mediaSize = PrintAttributes.MediaSize(
        template.id.replace(" ", "_"),
        template.name,
        widthMils,
        heightMils
    )
    return PrintAttributes.Builder()
        .setMediaSize(mediaSize)
        .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
}

private fun buildAveryPrintAttributes(config: LabelPrintConfig): PrintAttributes {
    val metrics = config.avery ?: throw IllegalStateException("Avery metrics required")
    val widthMils = mmToMils(metrics.pageWidthMm)
    val heightMils = mmToMils(metrics.pageHeightMm)
    val mediaSize = PrintAttributes.MediaSize(
        config.templateId,
        config.templateId,
        widthMils,
        heightMils
    )
    return PrintAttributes.Builder()
        .setMediaSize(mediaSize)
        .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
}

private fun spoolPdf(context: Context, pdfBytes: ByteArray, attrs: PrintAttributes, jobName: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val adapter = ByteArrayPrintAdapter("culsi_labels.pdf", pdfBytes)
    printManager.print(jobName, adapter, attrs)
}

private fun jobNameFor(items: List<LabelData>): String {
    return when {
        items.isEmpty() -> "Culsi Labels (Reprint)"
        items.size == 1 -> "Label - ${items.first().itemName}"
        else -> "FoodLogBatch"
    }
}

private fun pointsToMils(points: Int): Int = ((points / 72f) * 1000f).roundToInt()

fun mmToPoints(mm: Float): Int = (mm * 72f / 25.4f).roundToInt()

private fun mmToMils(mm: Float): Int = ((mm / 25.4f) * 1000f).roundToInt()

private class ByteArrayPrintAdapter(
    private val fileName: String,
    private val data: ByteArray,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(fileName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pageRanges: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }
        destination?.fileDescriptor?.let { fd ->
            FileOutputStream(fd).use { it.write(data) }
        }
        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }
}
