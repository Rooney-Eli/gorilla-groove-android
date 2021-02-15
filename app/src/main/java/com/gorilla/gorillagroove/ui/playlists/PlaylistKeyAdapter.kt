package com.gorilla.gorillagroove.ui.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.model.PlaylistKey
import kotlinx.android.synthetic.main.playlists_info_item.view.*
import java.util.*



class PlaylistKeyAdapter(
    private val playlistKeyListener: OnPlaylistListener
) : RecyclerView.Adapter<PlaylistKeyAdapter.PlaylistViewHolder>(), Filterable {

    val TAG = "AppDebug"
    var playlistKeyList = mutableListOf<PlaylistKey>()

    fun submitPlaylistMap(playlistKeys: List<PlaylistKey>) {
        playlistKeyList.clear()
        playlistKeyList.addAll(playlistKeys)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.playlists_info_item, parent, false
        )
        return PlaylistViewHolder(itemView)
    }

    override fun getItemCount() = playlistKeyList.size

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.playlistName.text = playlistKeyList[position].name
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val playlistName: TextView = itemView.playlist_name
        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition

            //in event of animation
            if(position != RecyclerView.NO_POSITION) {
                playlistKeyListener.onPlaylistClick(position)

            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION) {
                playlistKeyListener.onPlaylistLongClick(position)
            }
            return true
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val resultsList : List<PlaylistKey> =
                if (constraint.isNullOrEmpty()) {
                    playlistKeyList
                } else {
                    val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                    playlistKeyList.filter {
                        it.name.toLowerCase(Locale.ROOT).contains(filterPattern)
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
