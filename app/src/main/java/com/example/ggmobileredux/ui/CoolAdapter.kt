package com.example.ggmobileredux.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.ggmobileredux.R
import com.example.ggmobileredux.model.Track
import kotlinx.android.synthetic.main.playlist_track_info_item.view.*
import kotlinx.android.synthetic.main.playlist_track_name_item.view.*
import kotlinx.android.synthetic.main.playlist_track_name_item.view.track_name


class CoolAdapter(
    private val listener: OnTrackListener
): RecyclerView.Adapter<CoolAdapter.PlaylistViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    var tracks: List<Track>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        return PlaylistViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.playlist_track_info_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return tracks.size
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val currentTrack = tracks[position]
        holder.tvArtist.text = currentTrack.artist
        holder.tvDuration.text = currentTrack.length.getSongTimeFromSeconds()
        holder.tvName.text = currentTrack.name
        holder.tvAlbum.text = currentTrack.album

    }

    private fun Long.getSongTimeFromSeconds(): String {
        val minutes = this / 60
        val seconds = this % 60
        return "$minutes:${String.format("%02d", seconds)}"
    }


    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val tvArtist: TextView = itemView.track_artist
        val tvDuration: TextView = itemView.track_duration
        val tvName: TextView = itemView.track_name
        val tvAlbum: TextView = itemView.track_album

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION) {
                listener.onTrackClick(position)
            }
        }
    }

    interface OnTrackListener {
        fun onTrackClick(position: Int)
    }

}