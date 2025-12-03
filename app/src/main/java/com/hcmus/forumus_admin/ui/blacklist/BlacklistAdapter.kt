package com.hcmus.forumus_admin.ui.blacklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.UserStatus

class BlacklistAdapter(
    private var users: List<BlacklistedUser>,
    private val onRemoveClick: (BlacklistedUser) -> Unit,
    private val onStatusClick: (BlacklistedUser) -> Unit
) : RecyclerView.Adapter<BlacklistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userAvatar: ImageView = view.findViewById(R.id.userAvatar)
        val userName: TextView = view.findViewById(R.id.userName)
        val userId: TextView = view.findViewById(R.id.userId)
        val statusBadge: TextView = view.findViewById(R.id.statusBadge)
        val removeButton: TextView = view.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blacklist_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        
        holder.userName.text = user.name
        holder.userId.text = user.id
        
        // Set status badge based on user status
        when (user.status) {
            UserStatus.BAN -> {
                holder.statusBadge.text = holder.itemView.context.getString(R.string.ban)
                holder.statusBadge.setBackgroundResource(R.drawable.bg_badge_ban)
                holder.statusBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.danger_red)
                )
            }
            UserStatus.WARNING -> {
                holder.statusBadge.text = holder.itemView.context.getString(R.string.warning)
                holder.statusBadge.setBackgroundResource(R.drawable.bg_badge_warning)
                holder.statusBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.warning_orange)
                )
            }
            UserStatus.REMIND -> {
                holder.statusBadge.text = holder.itemView.context.getString(R.string.remind)
                holder.statusBadge.setBackgroundResource(R.drawable.bg_badge_remind)
                holder.statusBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.primary_blue)
                )
            }
            UserStatus.NORMAL -> {
                // This case shouldn't appear in blacklist, but handle it for completeness
                holder.statusBadge.text = "Normal"
                holder.statusBadge.setBackgroundResource(R.drawable.bg_badge_remind)
                holder.statusBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
                )
            }
        }
        
        // Handle remove button click
        holder.removeButton.setOnClickListener {
            onRemoveClick(user)
        }
        
        // Handle status badge click to change status
        holder.statusBadge.setOnClickListener {
            onStatusClick(user)
        }
        
        // TODO: Load avatar image from URL using Glide or similar image loading library
        // For now, using default avatar
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<BlacklistedUser>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
