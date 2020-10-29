package com.example.ggmobileredux

import android.content.SharedPreferences
import com.example.ggmobileredux.database.CacheMapper
import com.example.ggmobileredux.database.DatabaseDao
import com.example.ggmobileredux.network.NetworkApi
import com.example.ggmobileredux.network.NetworkMapper
import com.example.ggmobileredux.network.track.TrackLinkResponse
import com.example.ggmobileredux.repository.MainRepository
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun `whatever`() {
    }
}