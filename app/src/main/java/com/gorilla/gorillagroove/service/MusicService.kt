package com.gorilla.gorillagroove.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.gorilla.gorillagroove.repository.MainRepository
import com.gorilla.gorillagroove.util.Constants.MEDIA_ROOT_ID
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


private const val TAG = "AppDebug: Music Service"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var repo: MainRepository

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForegroundService = false

    private val musicPlayerEventListener = PlayerEventListener()
    
    companion object {
        var curSongDuration = 0L
            private set
    }

    override fun onCreate() {
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
            ),
            repo
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
                    //Log.d(TAG, "onPlayerStateChanged: Awaiting media source...")
                    musicNotificationManager.hideNotification()
                }
                //Player.STATE_BUFFERING -> //Log.d(TAG, "onPlayerStateChanged: Buffering...")
                Player.STATE_READY -> {
                //Log.d(TAG, "onPlayerStateChanged: Ready!")


                    musicNotificationManager.showNotification(exoPlayer)
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) {
                            stopForeground(false)
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    //Log.d(TAG, "onPlayerStateChanged: Finished Playing.")

                    musicNotificationManager.hideNotification()
                }
                else -> {
                    musicNotificationManager.hideNotification()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            //Log.d(TAG, "onPlayerError: A player error has occurred $error")
        }

        override fun onPositionDiscontinuity(reason: Int) {
            super.onPositionDiscontinuity(reason)
            val sourceIndex: Int = exoPlayer.currentWindowIndex

            when(reason) {
                Player.DISCONTINUITY_REASON_PERIOD_TRANSITION -> {
                    //Log.d(TAG, "onPositionDiscontinuity: next track window")
                }
                Player.DISCONTINUITY_REASON_SEEK -> {
                    //Log.d(TAG, "onPositionDiscontinuity: track seeked")
                }
                Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> {
                    //Log.d(TAG, "onPositionDiscontinuity: unable to seek to location")
                }
                Player.DISCONTINUITY_REASON_INTERNAL -> {
                    //Log.d(TAG, "onPositionDiscontinuity: reason internal")
                }
                Player.DISCONTINUITY_REASON_AD_INSERTION -> {
                    //Log.d(TAG, "onPositionDiscontinuity: AD insertion...")
                }
            }
        }
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return repo.nowPlayingMetadataList[windowIndex].description
        }
    }

    private inner class MusicPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_URI or
                    PlaybackStateCompat.ACTION_PLAY_FROM_URI or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE

        override fun onPrepare(playWhenReady: Boolean) = Unit
        override fun onPrepareFromMediaId( mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
            
            val itemToPlay = repo.nowPlayingMetadataList.find { it.id == mediaId }
            val songIndex = if (itemToPlay == null) 0 else repo.nowPlayingMetadataList.indexOf(itemToPlay)
            
            if(repo.dataSetChanged) {
                preparePlayer(repo.nowPlayingConcatenatingMediaSource, songIndex, true)
                repo.dataSetChanged = false
            } else {
                exoPlayer.seekTo(songIndex, 0)
                exoPlayer.playWhenReady = true
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
        concatSource: ConcatenatingMediaSource,
        songIndex: Int,
        playWhenReady: Boolean
    ) {
        exoPlayer.stop(true)
        exoPlayer.setMediaSource(concatSource)
        exoPlayer.prepare() // triggers buffering
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.seekTo(songIndex, 0) //triggers seeked
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

    override fun onGetRoot( clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren( parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>> ) {
        when(parentId) {
            MEDIA_ROOT_ID -> {
                result.sendResult(null)
            }
        }
    }
}