package com.hcmus.forumus_admin.ui.totalusers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.User
import com.hcmus.forumus_admin.data.model.UserStatus

class TotalUsersAdapter(
    private val onUserClick: ((User) -> Unit)? = null
) : ListAdapter<User, TotalUsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_list, parent, false)
        return UserViewHolder(view, onUserClick)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateUsers(users: List<User>) {
        submitList(users)
    }

    class UserViewHolder(
        itemView: View,
        private val onUserClick: ((User) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val userAvatar: ImageView = itemView.findViewById(R.id.userAvatar)
        private val userName: TextView = itemView.findViewById(R.id.userName)
        private val userId: TextView = itemView.findViewById(R.id.userId)
        private val userStatus: TextView = itemView.findViewById(R.id.userStatus)

        fun bind(user: User) {
            userName.text = user.name
            userId.text = user.id

            // Set status badge
            when (user.status) {
                UserStatus.BANNED -> {
                    userStatus.text = itemView.context.getString(R.string.ban)
                    userStatus.setBackgroundResource(R.drawable.bg_badge_ban)
                    userStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.badge_ban_text)
                    )
                }
                UserStatus.WARNED -> {
                    userStatus.text = itemView.context.getString(R.string.warning)
                    userStatus.setBackgroundResource(R.drawable.bg_badge_warning)
                    userStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.badge_warning_text)
                    )
                }
                UserStatus.REMINDED -> {
                    userStatus.text = itemView.context.getString(R.string.remind)
                    userStatus.setBackgroundResource(R.drawable.bg_badge_remind)
                    userStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.badge_remind_text)
                    )
                }
                UserStatus.NORMAL -> {
                    userStatus.text = itemView.context.getString(R.string.normal)
                    userStatus.setBackgroundResource(R.drawable.bg_badge_normal)
                    userStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.badge_normal_text)
                    )
                }
            }

            // Load avatar using Glide
            if (!user.avatarUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(user.avatarUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(userAvatar)
            } else {
                userAvatar.setImageResource(R.drawable.ic_default_avatar)
            }

            // Set click listener
            itemView.setOnClickListener {
                onUserClick?.invoke(user)
            }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}

