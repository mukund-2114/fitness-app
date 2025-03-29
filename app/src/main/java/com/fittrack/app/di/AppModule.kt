package com.fittrack.app.di

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.fittrack.app.data.database.AppDatabase
import com.fittrack.app.data.database.WorkoutDao
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.util.NetworkUtils
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(database: AppDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        workoutDao: WorkoutDao,
        networkUtils: NetworkUtils
    ): WorkoutRepository {
        return WorkoutRepository(workoutDao, networkUtils)
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    @Provides
    @Singleton
    fun provideBillingClient(@ApplicationContext context: Context): BillingClient {
        return BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .build()
    }
}