package com.chris.culsi.print

import com.chris.culsi.AveryTemplateMetrics
import com.chris.culsi.TemplateType
import kotlin.math.roundToInt

data class LabelTemplate(
    val id: String,
    val name: String,
    val templateType: TemplateType,
    val pageWidthPts: Int,
    val pageHeightPts: Int,
    val cols: Int,
    val rows: Int,
    val labelWidthPts: Int,
    val labelHeightPts: Int,
    val pageLeftMarginPts: Int,
    val pageTopMarginPts: Int,
    val colGutterPts: Int,
    val rowGutterPts: Int,
    val defaultFontTitlePts: Float,
    val defaultFontBodyPts: Float,
    val titleLineSpacingPts: Float = defaultFontTitlePts * 1.15f,
    val bodyLineSpacingPts: Float = defaultFontBodyPts * 1.2f,
    val contentPaddingPts: Float = 8f,
    val averyMetrics: AveryTemplateMetrics? = null
) {
    val labelsPerPage: Int get() = cols * rows
}

object LabelTemplates {
    private fun inchesToPoints(value: Float): Int = (value * 72f).roundToInt()
    private fun inchesToMm(value: Float): Float = value * 25.4f

    val Avery5160 = LabelTemplate(
        id = "avery_5160",
        name = "Avery 5160 (30-up)",
        templateType = TemplateType.AVERY_SHEET,
        pageWidthPts = inchesToPoints(8.5f),
        pageHeightPts = inchesToPoints(11f),
        cols = 3,
        rows = 10,
        labelWidthPts = inchesToPoints(2.625f),
        labelHeightPts = inchesToPoints(1f),
        pageLeftMarginPts = inchesToPoints(0.1875f),
        pageTopMarginPts = inchesToPoints(0.5f),
        colGutterPts = inchesToPoints(0.125f),
        rowGutterPts = 0,
        defaultFontTitlePts = 12f,
        defaultFontBodyPts = 9f,
        contentPaddingPts = 10f,
        averyMetrics = AveryTemplateMetrics(
            pageWidthMm = inchesToMm(8.5f),
            pageHeightMm = inchesToMm(11f),
            rows = 10,
            cols = 3,
            labelWidthMm = inchesToMm(2.625f),
            labelHeightMm = inchesToMm(1f),
            marginTopMm = inchesToMm(0.5f),
            marginLeftMm = inchesToMm(0.1875f),
            hPitchMm = inchesToMm(2.75f),
            vPitchMm = inchesToMm(1f)
        )
    )

    val Avery5163 = LabelTemplate(
        id = "avery_5163",
        name = "Avery 5163 (10-up)",
        templateType = TemplateType.AVERY_SHEET,
        pageWidthPts = inchesToPoints(8.5f),
        pageHeightPts = inchesToPoints(11f),
        cols = 2,
        rows = 5,
        labelWidthPts = inchesToPoints(4f),
        labelHeightPts = inchesToPoints(2f),
        pageLeftMarginPts = inchesToPoints(0.15625f),
        pageTopMarginPts = inchesToPoints(0.5f),
        colGutterPts = inchesToPoints(0.125f),
        rowGutterPts = 0,
        defaultFontTitlePts = 18f,
        defaultFontBodyPts = 12f,
        contentPaddingPts = 18f,
        averyMetrics = AveryTemplateMetrics(
            pageWidthMm = inchesToMm(8.5f),
            pageHeightMm = inchesToMm(11f),
            rows = 5,
            cols = 2,
            labelWidthMm = inchesToMm(4f),
            labelHeightMm = inchesToMm(2f),
            marginTopMm = inchesToMm(0.5f),
            marginLeftMm = inchesToMm(0.15625f),
            hPitchMm = inchesToMm(4.125f),
            vPitchMm = inchesToMm(2f)
        )
    )

    val Avery5167 = LabelTemplate(
        id = "avery_5167",
        name = "Avery 5167 (80-up)",
        templateType = TemplateType.AVERY_SHEET,
        pageWidthPts = inchesToPoints(8.5f),
        pageHeightPts = inchesToPoints(11f),
        cols = 4,
        rows = 20,
        labelWidthPts = inchesToPoints(1.75f),
        labelHeightPts = inchesToPoints(0.5f),
        pageLeftMarginPts = inchesToPoints(0.25f),
        pageTopMarginPts = inchesToPoints(0.5f),
        colGutterPts = inchesToPoints(0.25f),
        rowGutterPts = 0,
        defaultFontTitlePts = 8f,
        defaultFontBodyPts = 6.5f,
        contentPaddingPts = 6f,
        averyMetrics = AveryTemplateMetrics(
            pageWidthMm = inchesToMm(8.5f),
            pageHeightMm = inchesToMm(11f),
            rows = 20,
            cols = 4,
            labelWidthMm = inchesToMm(1.75f),
            labelHeightMm = inchesToMm(0.5f),
            marginTopMm = inchesToMm(0.5f),
            marginLeftMm = inchesToMm(0.25f),
            hPitchMm = inchesToMm(2f),
            vPitchMm = inchesToMm(0.5f)
        )
    )

    val all: List<LabelTemplate> = listOf(Avery5160, Avery5163, Avery5167)
    val default: LabelTemplate = Avery5160

    fun findByName(name: String): LabelTemplate? = all.firstOrNull { it.name == name }
    fun findById(id: String): LabelTemplate? = all.firstOrNull { it.id == id }
}
