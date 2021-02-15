package com.gorilla.gorillagroove.di

import com.gorilla.gorillagroove.network.NetworkApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton


@Module
@InstallIn(ApplicationComponent::class)
object NetworkModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OkHttpClientProvider

    @Singleton
    @Provides
    fun provideGsonBuilder(): Gson {
        return GsonBuilder()
            .create()
    }

    @Singleton
    @Provides
    fun provideRetrofit(gson: Gson): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl("https://gorillagroove.net/")
            .addConverterFactory(GsonConverterFactory.create(gson))
    }

    @Singleton
    @Provides
    fun provideTrackService(retrofit: Retrofit.Builder): NetworkApi {
        return retrofit
            .build()
            .create(NetworkApi::class.java)
    }

    @Singleton
    @Provides
    @OkHttpClientProvider
    fun provideOkHttpClient(): OkHttpClient {
           return  OkHttpClient.Builder()
                .build()
    }
}