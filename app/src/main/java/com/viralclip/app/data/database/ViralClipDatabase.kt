package com.viralclip.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.viralclip.app.data.database.dao.*
import com.viralclip.app.data.database.entities.*

@Database(
    entities = [
        ProjectEntity::class,
        ClipEntity::class,
        CaptionEntity::class,
        TemplateEntity::class,
        BrandPresetEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ViralClipDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun clipDao(): ClipDao
    abstract fun captionDao(): CaptionDao
    abstract fun templateDao(): TemplateDao
    abstract fun brandPresetDao(): BrandPresetDao
}
