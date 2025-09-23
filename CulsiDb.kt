package com.chris.culsi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chris.culsi.AverySheetState
import com.chris.culsi.AverySheetStateDao

@Database(entities = [FoodLog::class, AverySheetState::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CulsiDb : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun averySheetStateDao(): AverySheetStateDao

    companion object {
        @Volatile private var INSTANCE: CulsiDb? = null
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `avery_sheet_state`(
                        `templateId` TEXT NOT NULL PRIMARY KEY,
                        `rows` INTEGER NOT NULL,
                        `cols` INTEGER NOT NULL,
                        `usedMask` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        fun get(context: Context): CulsiDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CulsiDb::class.java,
                    "culsi.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
