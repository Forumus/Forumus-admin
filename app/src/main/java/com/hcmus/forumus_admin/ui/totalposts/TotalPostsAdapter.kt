package com.hcmus.forumus_admin.ui.totalposts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.Post

class TotalPostsAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, TotalPostsAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_card, parent, false)
        return PostViewHolder(view, onPostClick)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updatePosts(posts: List<Post>) {
        submitList(posts)
    }

    class PostViewHolder(
        itemView: View,
        private val onPostClick: (Post) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val postTitle: TextView = itemView.findViewById(R.id.postTitle)
        private val postAuthor: TextView = itemView.findViewById(R.id.postAuthor)
        private val postDate: TextView = itemView.findViewById(R.id.postDate)
        private val postCategories: TextView = itemView.findViewById(R.id.postCategories)
        private val postDescription: TextView = itemView.findViewById(R.id.postDescription)

        fun bind(post: Post) {
            postTitle.text = post.title
            postAuthor.text = itemView.context.getString(R.string.by_author, post.author)
            postDate.text = post.date
            postDescription.text = post.description

            // Format categories as breadcrumb (e.g., "Category 1 › Category 2 › Category 3")
            val categoriesText = post.tags.joinToString(" › ") { it.name }
            postCategories.text = categoriesText

            itemView.setOnClickListener {
                onPostClick(post)
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
