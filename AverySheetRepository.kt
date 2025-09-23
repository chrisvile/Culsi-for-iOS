package com.chris.culsi

class AverySheetRepository(private val dao: AverySheetStateDao) {
    suspend fun getOrInit(config: LabelPrintConfig): AverySheetState? {
        if (config.templateType != TemplateType.AVERY_SHEET || config.avery == null) return null
        val existing = dao.get(config.templateId)
        if (existing != null) return existing
        val total = config.avery.rows * config.avery.cols
        val blank = "0".repeat(total)
        val init = AverySheetState(
            templateId = config.templateId,
            rows = config.avery.rows,
            cols = config.avery.cols,
            usedMask = blank
        )
        dao.upsert(init)
        return init
    }

    suspend fun save(state: AverySheetState) = dao.upsert(state)
    suspend fun clear(templateId: String) = dao.clear(templateId)
}
