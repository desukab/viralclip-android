package com.viralclip.app.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.viralclip.app.data.database.ViralClipDatabase
import com.viralclip.app.data.database.dao.*
import com.viralclip.app.data.preferences.UserPreferencesManager
import com.viralclip.app.data.repository.*
import com.viralclip.app.domain.repository.*
import com.viralclip.app.util.FileStorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ViralClipDatabase {
        return Room.databaseBuilder(
            context,
            ViralClipDatabase::class.java,
            ViralClipDatabase.DB_NAME
        )
            .addMigrations(ViralClipDatabase.MIGRATION_1_2, ViralClipDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides fun provideProjectDao(db: ViralClipDatabase): ProjectDao = db.projectDao()
    @Provides fun provideClipDao(db: ViralClipDatabase): ClipDao = db.clipDao()
    @Provides fun provideCaptionDao(db: ViralClipDatabase): CaptionDao = db.captionDao()
    @Provides fun provideTemplateDao(db: ViralClipDatabase): TemplateDao = db.templateDao()
    @Provides fun provideBrandPresetDao(db: ViralClipDatabase): BrandPresetDao = db.brandPresetDao()
    @Provides fun provideProcessingJobDao(db: ViralClipDatabase): ProcessingJobDao = db.processingJobDao()
    @Provides fun provideExportedAssetDao(db: ViralClipDatabase): ExportedAssetDao = db.exportedAssetDao()

    @Provides
    @Singleton
    fun provideUserPreferencesManager(@ApplicationContext context: Context): UserPreferencesManager {
        return UserPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideFileStorageManager(@ApplicationContext context: Context): FileStorageManager {
        return FileStorageManager(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    // ─── Core Repositories ─────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        clipDao: ClipDao
    ): ProjectRepository = ProjectRepositoryImpl(projectDao, clipDao)

    @Provides
    @Singleton
    fun provideClipRepository(dao: ClipDao): ClipRepository = ClipRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCaptionRepository(dao: CaptionDao): CaptionRepository = CaptionRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTemplateRepository(dao: TemplateDao): TemplateRepository = TemplateRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideBrandPresetRepository(dao: BrandPresetDao): BrandPresetRepository =
        BrandPresetRepositoryImpl(dao)

    // ─── Additional Repositories (stub implementations) ────────────────

    @Provides
    @Singleton
    fun provideVideoRepository(): VideoRepository = VideoRepositoryImpl()

    @Provides
    @Singleton
    fun provideExportRepository(): ExportRepository = ExportRepositoryImpl()

    @Provides
    @Singleton
    fun provideSettingsRepository(
        preferencesManager: UserPreferencesManager
    ): SettingsRepository = SettingsRepositoryImpl(preferencesManager)

    @Provides
    @Singleton
    fun provideAnalyticsRepository(): AnalyticsRepository = AnalyticsRepositoryImpl()

    @Provides
    @Singleton
    fun provideCacheRepository(): CacheRepository = CacheRepositoryImpl()

    @Provides
    @Singleton
    fun provideErrorRepository(): ErrorRepository = ErrorRepositoryImpl()
}
