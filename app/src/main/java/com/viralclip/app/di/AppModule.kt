package com.viralclip.app.di

import android.content.Context
import androidx.room.Room
import com.viralclip.app.data.database.ViralClipDatabase
import com.viralclip.app.data.database.dao.ClipDao
import com.viralclip.app.data.database.dao.ProjectDao
import com.viralclip.app.data.database.dao.CaptionDao
import com.viralclip.app.data.database.dao.TemplateDao
import com.viralclip.app.data.database.dao.BrandPresetDao
import com.viralclip.app.data.preferences.AppPreferences
import com.viralclip.app.data.preferences.UserPreferencesManager
import com.viralclip.app.data.repository.ClipRepositoryImpl
import com.viralclip.app.data.repository.ProjectRepositoryImpl
import com.viralclip.app.data.repository.CaptionRepositoryImpl
import com.viralclip.app.data.repository.TemplateRepositoryImpl
import com.viralclip.app.data.repository.BrandPresetRepositoryImpl
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.domain.repository.ProjectRepository
import com.viralclip.app.domain.repository.CaptionRepository
import com.viralclip.app.domain.repository.TemplateRepository
import com.viralclip.app.domain.repository.BrandPresetRepository
import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.core.audio.AudioProcessor
import com.viralclip.app.core.ai.ViralityScorer
import com.viralclip.app.core.ai.CaptionGenerator
import com.viralclip.app.core.ai.FaceTracker
import com.viralclip.app.core.analysis.FrameAnalyzer
import com.viralclip.app.services.VideoProcessingPipeline
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
            "viralclip_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideProjectDao(db: ViralClipDatabase): ProjectDao = db.projectDao()
    @Provides fun provideClipDao(db: ViralClipDatabase): ClipDao = db.clipDao()
    @Provides fun provideCaptionDao(db: ViralClipDatabase): CaptionDao = db.captionDao()
    @Provides fun provideTemplateDao(db: ViralClipDatabase): TemplateDao = db.templateDao()
    @Provides fun provideBrandPresetDao(db: ViralClipDatabase): BrandPresetDao = db.brandPresetDao()

    @Provides
    @Singleton
    fun provideUserPreferencesManager(@ApplicationContext context: Context): UserPreferencesManager {
        return UserPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideFFmpegProcessor(@ApplicationContext context: Context): FFmpegProcessor {
        return FFmpegProcessor(context)
    }

    @Provides
    @Singleton
    fun provideAudioProcessor(@ApplicationContext context: Context): AudioProcessor {
        return AudioProcessor(context)
    }

    @Provides
    @Singleton
    fun provideViralityScorer(@ApplicationContext context: Context): ViralityScorer {
        return ViralityScorer(context)
    }

    @Provides
    @Singleton
    fun provideCaptionGenerator(@ApplicationContext context: Context): CaptionGenerator {
        return CaptionGenerator(context)
    }

    @Provides
    @Singleton
    fun provideFaceTracker(@ApplicationContext context: Context): FaceTracker {
        return FaceTracker(context)
    }

    @Provides
    @Singleton
    fun provideFrameAnalyzer(@ApplicationContext context: Context): FrameAnalyzer {
        return FrameAnalyzer(context)
    }

    // Repositories
    @Provides @Singleton
    fun provideProjectRepository(dao: ProjectDao): ProjectRepository = ProjectRepositoryImpl(dao)

    @Provides @Singleton
    fun provideClipRepository(dao: ClipDao): ClipRepository = ClipRepositoryImpl(dao)

    @Provides @Singleton
    fun provideCaptionRepository(dao: CaptionDao): CaptionRepository = CaptionRepositoryImpl(dao)

    @Provides @Singleton
    fun provideTemplateRepository(dao: TemplateDao): TemplateRepository = TemplateRepositoryImpl(dao)

    @Provides @Singleton
    fun provideBrandPresetRepository(dao: BrandPresetDao): BrandPresetRepository = BrandPresetRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideVideoProcessingPipeline(
        ffmpegProcessor: FFmpegProcessor,
        audioProcessor: AudioProcessor,
        viralityScorer: ViralityScorer,
        captionGenerator: CaptionGenerator,
        faceTracker: FaceTracker,
        frameAnalyzer: FrameAnalyzer
    ): VideoProcessingPipeline {
        return VideoProcessingPipeline(
            ffmpegProcessor, audioProcessor, viralityScorer,
            captionGenerator, faceTracker, frameAnalyzer
        )
    }
}
