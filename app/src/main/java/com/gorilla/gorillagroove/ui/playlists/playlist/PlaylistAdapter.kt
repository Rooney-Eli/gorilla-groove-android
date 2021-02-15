package com.gorilla.gorillagroove.ui.playlists.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.model.Playlist
import com.gorilla.gorillagroove.model.PlaylistItem
import kotlinx.android.synthetic.main.playlist_track_info_item.view.*
import kotlinx.android.synthetic.main.playlist_track_info_item.view.track_name
import java.util.*



class PlaylistAdapter(
    private val playlistListener: OnPlaylistListener
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>(), Filterable {

    val TAG = "AppDebug"

    var playlistItems = mutableListOf<PlaylistItem>()
    var showingCheckBox = false

    fun submitPlaylist(playlist: Playlist) {
        playlistItems.addAll(playlist.playlistItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.playlist_track_info_item, parent, false
        )
        return PlaylistViewHolder(itemView)
    }

    override fun getItemCount() = playlistItems.size

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val currentPlaylistItem = playlistItems[position]
        holder.tvArtist.text = currentPlaylistItem.track.artist
        holder.tvDuration.text = currentPlaylistItem.track.length.getSongTimeFromSeconds()
        holder.tvName.text = currentPlaylistItem.track.name
        holder.tvAlbum.text = currentPlaylistItem.track.album

        holder.checkbox.isVisible = showingCheckBox
        //holder.checkbox.isChecked = checkedTracks[filteredList[position].id] ?: false
        holder.checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(buttonView.isShown) {
                buttonView.isChecked = isChecked
            //    checkedTracks[filteredList[position].id] = isChecked
            }
        }
    }

    private fun Long.getSongTimeFromSeconds(): String {
        val minutes = this / 60
        val seconds = this % 60
        return "$minutes:${String.format("%02d", seconds)}"
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val tvArtist: TextView = itemView.track_artist
        val tvDuration: TextView = itemView.track_duration
        val tvName: TextView = itemView.track_name
        val tvAlbum: TextView = itemView.track_album
        val checkbox: CheckBox = itemView.checkbox

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            //in event of animation
            if(showingCheckBox){
                checkbox.isChecked = !checkbox.isChecked
            } else {
                if(position != RecyclerView.NO_POSITION) {
                    playlistListener.onPlaylistClick(position)
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION) {
                playlistListener.onPlaylistLongClick(position)
            }
            return true
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val resultsList : List<PlaylistItem> =
                    if (constraint.isNullOrEmpty()) {
                        playlistItems
                    } else {
                        val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                        playlistItems.filter {
                            it.track.name.toLowerCase(Locale.ROOT).contains(filterPattern)
                        }
                    }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }

    interface OnPlaylistListener {
        fun onPlaylistClick(position: Int)
        fun onPlaylistLongClick(position: Int) : Boolean
    }
}
