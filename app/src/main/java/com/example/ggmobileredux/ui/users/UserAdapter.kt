package com.example.ggmobileredux.ui.users

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
import com.example.ggmobileredux.model.User
import kotlinx.android.synthetic.main.user_info_item.view.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class UserAdapter(
    private val listener: OnUserListener
) : RecyclerView.Adapter<UserAdapter.PlaylistViewHolder>(), Filterable{

    var userList = listOf<User>()


    fun submitList(users: List<User>) {
        userList = users
        notifyDataSetChanged()

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.user_info_item, parent, false
        )
        return PlaylistViewHolder(itemView)
    }

    override fun getItemCount() = userList.size

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.tvUsername.text = currentUser.username

    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        val tvUsername: TextView = itemView.user_name


        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition

            //in event of animation
            if(position != RecyclerView.NO_POSITION) {
                listener.onUserClick(position)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION) {
                listener.onUserLongClick(position)
            }
            return true
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val resultsList : List<User> =
                if (constraint.isNullOrEmpty()) {
                    userList
                } else {
                    val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                    userList.filter {
                        it.username.toLowerCase(Locale.ROOT).contains(filterPattern)
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

    interface OnUserListener {
        fun onUserClick(position: Int)
        fun onUserLongClick(position: Int) : Boolean
    }
}
