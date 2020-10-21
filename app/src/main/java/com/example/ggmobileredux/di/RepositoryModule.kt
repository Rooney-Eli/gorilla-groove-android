package com.example.ggmobileredux.di

import android.content.SharedPreferences
import com.example.ggmobileredux.repository.MainRepository
import com.example.ggmobileredux.network.NetworkMapper
import com.example.ggmobileredux.network.NetworkApi
import com.example.ggmobileredux.database.CacheMapper
import com.example.ggmobileredux.database.DatabaseDao
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object RepositoryModule {

    @Singleton
    @Provides
    fun provideMainRepository(
        databaseDao: DatabaseDao,
        retrofit: NetworkApi,
        cacheMapper: CacheMapper,
        networkMapper: NetworkMapper,
        sharedPreferences: SharedPreferences,
        dataSourceFactory: DefaultDataSourceFactory,
        @NetworkModule.OkHttpClientProvider okClient: OkHttpClient
    ): MainRepository {
        return MainRepository(databaseDao, retrofit, cacheMapper, networkMapper, sharedPreferences, dataSourceFactory, okClient)
    }
}