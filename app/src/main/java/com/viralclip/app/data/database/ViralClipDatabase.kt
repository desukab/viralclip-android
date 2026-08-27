package com.viralclip.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.viralclip.app.data.database.dao.*
import com.viralclip.app.data.database.entities.*

@Database(
    entities = [
        ProjectEntity::class,
        ClipEntity::class,
        CaptionEntity::class,
        TemplateEntity::class,
        BrandPresetEntity::class,
        ProcessingJobEntity::class,
        ExportedAssetEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ViralClipDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun clipDao(): ClipDao
    abstract fun captionDao(): CaptionDao
    abstract fun templateDao(): TemplateDao
    abstract fun brandPresetDao(): BrandPresetDao
    abstract fun processingJobDao(): ProcessingJobDao
    abstract fun exportedAssetDao(): ExportedAssetDao

    companion object {
        const val DB_NAME = "viralclip_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE projects ADD COLUMN exportSettings TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL("ALTER TABLE projects ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN lastOpenedAt INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE clips ADD COLUMN exportedPath TEXT")
                db.execSQL("ALTER TABLE clips ADD COLUMN exportProgress REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clips ADD COLUMN isExported INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clips ADD COLUMN thumbnailPath TEXT")
                db.execSQL("ALTER TABLE clips ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clips ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE templates ADD COLUMN usageCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE templates ADD COLUMN rating REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE templates ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_templates_category ON templates(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_templates_isBuiltIn ON templates(isBuiltIn)")

                db.execSQL("ALTER TABLE brand_presets ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE captions ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_captions_startTimeMs ON captions(startTimeMs)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS processing_jobs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER,
                        clipId INTEGER,
                        jobType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        progress REAL NOT NULL,
                        inputUri TEXT NOT NULL,
                        outputPath TEXT,
                        errorMessage TEXT,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        retryCount INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_processing_jobs_status ON processing_jobs(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_processing_jobs_createdAt ON processing_jobs(createdAt)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exported_assets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        clipId INTEGER,
                        filePath TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        width INTEGER NOT NULL,
                        height INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        platform TEXT,
                        sharedTo TEXT,
                        exportedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exported_assets_projectId ON exported_assets(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exported_assets_clipId ON exported_assets(clipId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exported_assets_exportedAt ON exported_assets(exportedAt)")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_clips_viralityScore ON clips(viralityScore)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_clips_order ON clips(`order`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE brand_presets ADD COLUMN category TEXT")
            }
        }

        fun build(context: Context): ViralClipDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ViralClipDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}
