package com.example.ggmobileredux.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ggmobileredux.R
import com.example.ggmobileredux.model.Track
import kotlinx.android.synthetic.main.playlist_track_info_item.view.*
import kotlinx.android.synthetic.main.playlist_track_name_item.view.*
import kotlinx.android.synthetic.main.playlist_track_name_item.view.track_name
import java.util.*

class PlaylistAdapter(
    //private val trackList: List<Track>,
    private val listener: OnTrackListener
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>(), Filterable{

    var trackList = listOf<Track>()
    val filteredList: MutableList<Track> = trackList.toMutableList()


    fun submitList(tracks: List<Track>) {
        trackList = tracks
        filteredList.clear()
        filteredList.addAll(trackList)
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

            //in event of animation
            if(position != RecyclerView.NO_POSITION) {
                listener.onTrackClick(position)
            }

        }

    }

    interface OnTrackListener {
        fun onTrackClick(position: Int)
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
                        it.name.toLowerCase(Locale.ROOT).trim().contains(filterPattern) ||
                        it.artist.toLowerCase(Locale.ROOT).trim().contains(filterPattern)
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
}