package com.hcmus.forumus_admin.ui.blacklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
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
            UserStatus.BANNED -> {
                holder.statusBadge.text = holder.itemView.context.getString(R.string.ban)
                holder.statusBadge.setBackgroundResource(R.drawable.bg_badge_ban)
                holder.statusBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.badge_ban_text)
                )
            }
            UserStatus.WARNED -> {
                holder.statusBadge.text = holder.itemView.context.getString(R.string.warning)
                holder.statusBadge.setBackgroundResource(R.drawable.bg_badge_warning)
                holder.statusBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.badge_warning_text)
                )
            }
            UserStatus.REMINDED -> {
                holder.statusBadge.text = holder.itemView.context.getString(R.string.remind)
                holder.statusBadge.setBackgroundResource(R.drawable.bg_badge_remind)
                holder.statusBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.badge_remind_text)
                )
            }
            UserStatus.NORMAL -> {
                holder.statusBadge.text = "Normal"
                holder.statusBadge.setBackgroundResource(R.drawable.bg_badge_remind)
                holder.statusBadge.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
                )
            }
        }

        holder.removeButton.setOnClickListener {
            onRemoveClick(user)
        }

        holder.statusBadge.setOnClickListener {
            onStatusClick(user)
        }

        if (!user.avatarUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.avatarUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(holder.userAvatar)
        } else {
            holder.userAvatar.setImageResource(R.drawable.ic_default_avatar)
        }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<BlacklistedUser>) {
        users = newUsers
        notifyDataSetChanged()
    }
}

