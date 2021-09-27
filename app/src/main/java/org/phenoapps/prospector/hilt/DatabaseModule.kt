package org.phenoapps.prospector.hilt

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.dao.ExperimentDao
import org.phenoapps.prospector.data.dao.SampleDao
import org.phenoapps.prospector.data.dao.ScanDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideExperimentDao(db: ProspectorDatabase): ExperimentDao {
        return db.experimentDao()
    }

    @Singleton
    @Provides
    fun provideSampleDao(db: ProspectorDatabase): SampleDao {
        return db.sampleDao()
    }

    @Singleton
    @Provides
    fun provideScanDao(db: ProspectorDatabase): ScanDao {
        return db.scanDao()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): ProspectorDatabase {
        return ProspectorDatabase.getInstance(appContext)
    }
}