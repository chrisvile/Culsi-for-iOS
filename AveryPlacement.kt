package com.chris.culsi

data class CellIndex(val index: Int) // 0-based, row-major
data class PagePlacement(val cellIndices: List<CellIndex>)
data class PlacementPlan(val pages: List<PagePlacement>)

/**
 * Build a placement plan given a used mask and a desired number of labels to place.
 * If startRow/startCol provided, begin there; otherwise fill from first free cell in reading order.
 */
fun buildAveryPlacementPlan(
    rows: Int,
    cols: Int,
    usedMask: String,
    labelsNeeded: Int,
    startRow: Int? = null, // 1-based
    startCol: Int? = null  // 1-based
): PlacementPlan {
    require(usedMask.length == rows * cols)
    val total = rows * cols
    val used = usedMask.toCharArray()
    val startIdx = when {
        startRow != null && startCol != null -> {
            val r = (startRow - 1).coerceIn(0, rows - 1)
            val c = (startCol - 1).coerceIn(0, cols - 1)
            r * cols + c
        }
        else -> 0
    }

    val placements = mutableListOf<CellIndex>()
    var idx = startIdx
    while (placements.size < labelsNeeded && total > 0) {
        var loops = 0
        while (loops < total && used[idx] == '1') {
            idx = (idx + 1) % total
            loops++
        }
        if (loops >= total && used[idx] == '1') break // no free cells
        placements += CellIndex(idx)
        used[idx] = '1'
        idx = (idx + 1) % total
    }

    val perPage = total
    val pages = placements.chunked(perPage).map { PagePlacement(it) }

    return PlacementPlan(pages)
}

/** Apply the placements to the mask to produce the updated mask (after print). */
fun applyPlanToMask(
    usedMask: String,
    plan: PlacementPlan
): String {
    val arr = usedMask.toCharArray()
    plan.pages.flatMap { it.cellIndices }.forEach { cell ->
        if (cell.index in arr.indices) arr[cell.index] = '1'
    }
    return arr.concatToString()
}
