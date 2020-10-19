package com.example.ggmobileredux.ui.library

import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.util.keyIterator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.ggmobileredux.R
import com.example.ggmobileredux.model.Track
import kotlinx.android.synthetic.main.playlist_track_info_item.view.*
import kotlinx.android.synthetic.main.playlist_track_name_item.view.track_name
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class PlaylistAdapter(
    private val listener: OnTrackListener
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>(), Filterable{

    var trackList = listOf<Track>()
    val filteredList: MutableList<Track> = trackList.toMutableList()

    val checkedTracks = LinkedHashMap<Int, Boolean>()

    var showingCheckBox = false

    fun submitList(tracks: List<Track>) {
        trackList = tracks
        filteredList.clear()
        filteredList.addAll(trackList)
        notifyDataSetChanged()

    }

    fun getSelectedTracks(): List<Int> {
        val tracks = mutableListOf<Int>()
        for (id in checkedTracks.keys)
        {
            if(checkedTracks.getValue(id)) {
                tracks.add(id)
            }
        }
        return tracks
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.playlist_track_info_item, parent, false
        )
        return PlaylistViewHolder(itemView)
    }

    override fun getItemCount() = filteredList.size

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val currentTrack = filteredList[position]
        holder.tvArtist.text = currentTrack.artist
        holder.tvDuration.text = currentTrack.length.getSongTimeFromSeconds()
        holder.tvName.text = currentTrack.name
        holder.tvAlbum.text = currentTrack.album

        holder.checkbox.isVisible = showingCheckBox
        holder.checkbox.isChecked = checkedTracks[filteredList[position].id] ?: false
        holder.checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(buttonView.isShown) {
                buttonView.isChecked = isChecked
                checkedTracks[filteredList[position].id] = isChecked
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
                    listener.onTrackClick(position)
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION) {
                listener.onTrackLongClick(position)
            }
            return true
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val resultsList : List<Track> =
                if (constraint.isNullOrEmpty()) {
                    trackList
                } else {
                    val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                    trackList.filter {
                        it.name.toLowerCase(Locale.ROOT).contains(filterPattern) ||
                        it.artist.toLowerCase(Locale.ROOT).contains(filterPattern)
                    }
                }

                val filterResults = FilterResults()
                filterResults.values = resultsList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList.clear()
                filteredList.addAll(results?.values as List<Track>)
                notifyDataSetChanged()
            }
        }
    }

    interface OnTrackListener {
        fun onTrackClick(position: Int)
        fun onTrackLongClick(position: Int) : Boolean
    }
}
