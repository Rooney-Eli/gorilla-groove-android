package com.example.ggmobileredux.ui.library

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.RequestManager
import com.example.ggmobileredux.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val TAG = "AppDebug : MainActivity"

    @Inject
    lateinit var glide: RequestManager

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayShowTitleEnabled(false)


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController


        bottomNavigationView.setupWithNavController(navController)

        navHostFragment.findNavController()
            .addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.loginFragment -> {
                        bottomNavigationView.visibility = View.GONE
                        playerControlView.visibility = View.GONE
                        title_tv.text = ""
                    }
                    R.id.mainFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Library"
                    }
                    R.id.playingFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Now Playing"
                    }
                    R.id.usersFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Users"
                    }
                    R.id.playlistsFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Playlists"
                    }
                    R.id.settingsFragment -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "Settings"
                    }
                    else -> {
                        bottomNavigationView.visibility = View.VISIBLE
                        playerControlView.visibility = View.VISIBLE
                        title_tv.text = "var ar jag?"
                        supportActionBar?.displayOptions
                    }
                }
            }

        subscribeObservers()

        initProgressBar()
        playpause_button.setOnClickListener {
            viewModel.playPause()
        }
        audio_seek_bar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    Log.d(TAG, "onStartTrackingTouch: ")
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.skipTo(audio_seek_bar.progress.toLong() * 1000)
                }

            }
        )
    }

    private fun initProgressBar() {
        audio_seek_bar.min = 0
        audio_seek_bar.max = 100
        //audio_progress_bar.progress = 50
    }

    private fun subscribeObservers() {
        viewModel.playPauseState.observe(this, Observer {
            if (it.isPlaying) {
                playpause_button.setImageResource(R.drawable.ic_pause_24)
            } else {
                playpause_button.setImageResource(R.drawable.ic_play_arrow_24)
            }

        })

        viewModel.currentTrackItem.observe(this, Observer {
            now_playing_textview.text = it.description?.title
            track_duration_textview.text =
                it.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).getSongTimeFromMilliseconds()
            audio_seek_bar.max =
                it.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt() / 1000
        })

        viewModel.mediaPosition.observe(this, Observer {
            track_position_textview.text = it?.getSongTimeFromMilliseconds() ?: "0"
            audio_seek_bar.progress = (it?.toInt() ?: 0) / 1000
        })

    }
}

private fun Long.getSongTimeFromSeconds(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${String.format("%02d", seconds)}"
}

fun Long.getSongTimeFromMilliseconds(): String {
    return String.format(
        "%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(this),
        TimeUnit.MILLISECONDS.toSeconds(this) -
                TimeUnit.MINUTES.toSeconds((TimeUnit.MILLISECONDS.toMinutes(this))
                )
    )
}