package com.example.ggmobileredux.di

import android.content.SharedPreferences
import com.example.ggmobileredux.repository.MainRepository
import com.example.ggmobileredux.retrofit.NetworkMapper
import com.example.ggmobileredux.retrofit.TrackRetrofit
import com.example.ggmobileredux.room.CacheMapper
import com.example.ggmobileredux.room.TrackDao
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object RepositoryModule {

    @Singleton
    @Provides
    fun provideMainRepository(
        trackDao: TrackDao,
        retrofit: TrackRetrofit,
        cacheMapper: CacheMapper,
        networkMapper: NetworkMapper,
        sharedPreferences: SharedPreferences,
        dataSourceFactory: DefaultDataSourceFactory,
        @RetrofitModule.OkHttpClientProvider okClient: OkHttpClient
    ): MainRepository {
        return MainRepository(trackDao, retrofit, cacheMapper, networkMapper, sharedPreferences, dataSourceFactory, okClient)
    }
}