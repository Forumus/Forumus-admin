package com.anhkhoa.forumus_admin.ui.assistant

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anhkhoa.forumus_admin.databinding.ItemAiPostCardBinding
import com.anhkhoa.forumus_admin.data.model.Post
import com.anhkhoa.forumus_admin.data.model.Tag
import com.google.android.material.chip.Chip

class AiPostsAdapter(
    private val onApprove: (String) -> Unit,
    private val onReject: (String) -> Unit
) : ListAdapter<Post, AiPostsAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemAiPostCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding, onApprove, onReject)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(
        private val binding: ItemAiPostCardBinding,
        private val onApprove: (String) -> Unit,
        private val onReject: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.apply {
                postTitle.text = post.title
                postAuthor.text = "by ${post.author}"
                postDate.text = post.date
                postDescription.text = post.description

                // Clear previous tags
                tagsContainer.removeAllViews()

                // Add tags
                post.tags.forEach { tag ->
                    val chip = Chip(binding.root.context).apply {
                        text = tag.name
                        chipBackgroundColor = ContextCompat.getColorStateList(
                            context,
                            tag.backgroundColor
                        )
                        setTextColor(ContextCompat.getColor(context, tag.textColor))
                        isClickable = false
                        isCheckable = false
                    }
                    tagsContainer.addView(chip)
                }

                // Update approve button style based on post status
                // If post is AI rejected (not approved), show blue active style
                if (!post.isAiApproved) {
                    approveButton.setBackgroundResource(com.anhkhoa.forumus_admin.R.drawable.bg_approve_button_active)
                } else {
                    approveButton.setBackgroundResource(com.anhkhoa.forumus_admin.R.drawable.bg_approve_button)
                }

                // Set click listeners
                approveButton.setOnClickListener {
                    onApprove(post.id)
                }

                rejectButton.setOnClickListener {
                    onReject(post.id)
                }
            }
        }
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
