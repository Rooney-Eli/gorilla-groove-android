package com.example.ggmobileredux.service

import android.app.PendingIntent
import android.content.Intent
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import com.example.ggmobileredux.repository.MainRepository
import com.example.ggmobileredux.util.Constants.MEDIA_ROOT_ID
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val TAG = "AppDebug: Music Service"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var repo: MainRepository

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector //exoplayers

    var isForegroundService = false

    private var curPlayingSong: MediaMetadataCompat? = null

    private var isPlayerInitialized = false

    private val musicPlayerEventListener = PlayerEventListener()
    
    companion object {
        var curSongDuration = 0L
            private set
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate: Creating service...")
        super.onCreate()

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }



        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(
                this
            )
        ) {
            curSongDuration = exoPlayer.duration
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(MusicPlaybackPreparer())
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {



                    Log.d(TAG, "onPlayerStateChanged: Awaiting media source...")
                    musicNotificationManager.hideNotification()
                }
                Player.STATE_BUFFERING -> Log.d(TAG, "onPlayerStateChanged: Buffering...")
                Player.STATE_READY -> {
                    Log.d(TAG, "onPlayerStateChanged: Ready!")

                    musicNotificationManager.showNotification(exoPlayer)
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) {
                            stopForeground(false)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    Log.d(TAG, "onPlayerStateChanged: Finished Playing.")
                    musicNotificationManager.hideNotification()
                }
                else -> {
                    musicNotificationManager.hideNotification()
                }
            }
        }
        override fun onPlayerError(error: ExoPlaybackException) {
            Log.d(TAG, "onPlayerError: A player error has occurred")
        }
    }
    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return repo.recentSongs[windowIndex].description
        }
    }

    private inner class MusicPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_URI or
                    PlaybackStateCompat.ACTION_PLAY_FROM_URI

        override fun onPrepare(playWhenReady: Boolean) = Unit
        override fun onPrepareFromMediaId( mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
            val trackId = Integer.parseInt(mediaId)
            Log.d(TAG, "onPrepareFromMediaId: Attempting to retrieve $trackId from repo...")
            val track = repo.getLoadedSongById(trackId)

            track?.let {
                Log.d(TAG, "onPrepareFromMediaId: Successfully retrieved ${track.description.title}")
                repo.addMediaSource(track)

                //hardcoding playNow basically makes this a onPlayFromMediaId function
                preparePlayer(track, true)
            }
        }
        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit
        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit
        override fun onCommand(
            player: Player,
            controlDispatcher: ControlDispatcher,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ) = false
    }

    private fun preparePlayer(
        track: MediaMetadataCompat,
        playNow: Boolean
    ) {
        Log.d(TAG, "preparePlayer: Preparing player for ${track.description.title}...")
        curPlayingSong = track

        val initialWindowIndex = repo.recentSongs.indexOf(track)

        exoPlayer.prepare(repo.concatenatingMediaSource)
        isPlayerInitialized = true
        exoPlayer.seekTo(initialWindowIndex, 0L) // seek to the new playing song
        exoPlayer.playWhenReady = playNow
        Log.d(TAG, "preparePlayer: Player prepared!")
    }

    private fun preparePlayer(
        tracks: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        //TODO: support multi selections from UI layer
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId) {
            MEDIA_ROOT_ID -> {
                        result.sendResult(null)
                    }
                }
    }
}