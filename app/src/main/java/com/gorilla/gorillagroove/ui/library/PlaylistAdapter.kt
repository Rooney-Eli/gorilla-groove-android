package com.gorilla.gorillagroove.ui.library

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gorilla.gorillagroove.R
import com.gorilla.gorillagroove.model.Track
import kotlinx.android.synthetic.main.playlist_track_info_item.view.checkbox
import kotlinx.android.synthetic.main.playlist_track_info_item.view.playStatusButton
import kotlinx.android.synthetic.main.track_expandable_item.view.*
import java.util.*
import kotlin.collections.LinkedHashMap

class PlaylistAdapter(
    private val listener: OnTrackListener
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>(), Filterable{

    var trackList = listOf<Track>()
    val filteredList: MutableList<Track> = trackList.toMutableList()
    var playingTrackId: String? = null
    var isPlaying = false

    val checkedTracks = LinkedHashMap<Long, Boolean>()

    var showingCheckBox = false

    fun submitList(tracks: List<Track>) {
        trackList = tracks
        filteredList.clear()
        filteredList.addAll(trackList)
        notifyDataSetChanged()

    }

    fun getSelectedTracks(): List<Long> {
        val tracks = mutableListOf<Long>()
        for (id in checkedTracks.keys)
        {
            if(checkedTracks.getValue(id)) {
                tracks.add(id)
            }
        }

        return tracks
    }

    @ColorInt
    fun Context.getColorFromAttr(
        @AttrRes attrColor: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.track_expandable_item, parent, false
        )
        return PlaylistViewHolder(itemView)
    }

    override fun getItemCount() = filteredList.size

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val currentTrack = filteredList[position]
        holder.tvArtist.text = currentTrack.artist
        holder.tvName.text = currentTrack.name
        holder.tvAlbum.text = currentTrack.album

        if(currentTrack.id.toString() == playingTrackId) {
            val activeColor = holder.itemView.context.getColorFromAttr(R.attr.colorPrimary)
            holder.tvName.setTextColor(activeColor)
            holder.tvArtist.setTextColor(activeColor)
            holder.tvAlbum.setTextColor(activeColor)
            holder.imageButton.visibility = View.VISIBLE

        } else {


            holder.tvName.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.tvArtist.setTextColor(holder.itemView.context.getColor(android.R.color.tab_indicator_text))
            holder.tvAlbum.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.imageButton.visibility = View.GONE
        }

        if(isPlaying) {
            holder.imageButton.setImageResource(R.drawable.ic_pause_24)

        } else {
            holder.imageButton.setImageResource(R.drawable.ic_play_arrow_24)
        }


        holder.checkbox.isVisible = showingCheckBox
        holder.options.isVisible = !showingCheckBox
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
        val tvArtist: TextView = itemView.tv_artist
        val tvName: TextView = itemView.tv_title
        val tvAlbum: TextView = itemView.tv_album
        val imageButton: ImageButton = itemView.playStatusButton
        val checkbox: CheckBox = itemView.checkbox
        val options: TextView = itemView.tv_options
        val menu_button_parent: ConstraintLayout = itemView.menu_button_layout

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            imageButton.setOnClickListener {
                val position = adapterPosition
                if(position != RecyclerView.NO_POSITION) {
                    listener.onPlayPauseClick(position)
                }
            }
            options.setOnClickListener {
                val position = adapterPosition
                val popup = PopupMenu(itemView.context, it)
                popup.inflate(R.menu.track_floating_menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_play_next -> {
                            listener.onPlayNextSelection(position)
                            true
                        }
                        R.id.action_play_last -> {
                            listener.onPlayLastSelection(position)
                            true
                        }
                        R.id.action_get_link -> {
                            listener.onGetLinkSelection(position)
                            true
                        }
                        R.id.action_download -> {
                            listener.onDownloadSelection(position)
                            true
                        }
                        R.id.action_recommend -> {
                            listener.onRecommendSelection(position)
                            true
                        }
                        R.id.action_add_to_playlist -> {
                            listener.onAddToPlaylistSelection(position)
                            true
                        }
                        R.id.action_properties -> {
                            listener.onPropertiesSelection(position)
                            true
                        }
                        else -> false
                    }
                }

                popup.show()
            }

            expandViewHitArea(menu_button_parent, options)

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

        private fun expandViewHitArea(parent : View, child : View) {

            parent.post {

                val parentRect = Rect()
                val childRect = Rect()
                parent.getHitRect(parentRect)
                child.getHitRect(childRect)

                childRect.left = 0
                childRect.top = 0
                childRect.right = parentRect.width()
                childRect.bottom = parentRect.height()

                parent.touchDelegate = TouchDelegate(childRect, child)
            }
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
        fun onPlayPauseClick(position: Int)
        fun onOptionsClick(position: Int)

        fun onPlayNextSelection(position: Int)
        fun onPlayLastSelection(position: Int)
        fun onGetLinkSelection(position: Int)
        fun onDownloadSelection(position: Int)
        fun onRecommendSelection(position: Int)
        fun onAddToPlaylistSelection(position: Int)
        fun onPropertiesSelection(position: Int)
    }

//    interface OnOptionsMenuListener {
//        fun onPlayNextSelection(position: Int)
//        fun onPlayLastSelection(position: Int)
//        fun onGetLinkSelection(position: Int)
//        fun onDownloadSelection(position: Int)
//        fun onRecommendSelection(position: Int)
//        fun onAddToPlaylistSelection(position: Int)
//    }


}
