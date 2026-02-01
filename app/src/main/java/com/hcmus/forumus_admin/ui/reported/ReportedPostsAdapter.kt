package com.hcmus.forumus_admin.ui.reported

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_admin.R

class ReportedPostsAdapter(
    private var posts: List<ReportedPost>,
    private val onItemClick: (ReportedPost) -> Unit,
    private val onDismissClick: (ReportedPost) -> Unit,
    private val onDeleteClick: (ReportedPost) -> Unit,
    private val onViolationBadgeClick: (ReportedPost) -> Unit,
    private val onReportBadgeClick: (ReportedPost) -> Unit
) : RecyclerView.Adapter<ReportedPostsAdapter.ViewHolder>() {

    private var loadingPostIds: Set<String> = emptySet()

    fun setLoadingPostIds(ids: Set<String>) {
        val oldIds = loadingPostIds
        loadingPostIds = ids
        posts.forEachIndexed { index, post ->
            if ((post.id in oldIds) != (post.id in ids)) {
                notifyItemChanged(index, PAYLOAD_LOADING_STATE)
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.postTitle)
        val authorText: TextView = view.findViewById(R.id.postAuthor)
        val dateText: TextView = view.findViewById(R.id.postDate)
        val categoriesText: TextView = view.findViewById(R.id.postCategories)
        val descriptionText: TextView = view.findViewById(R.id.postDescription)
        val violationBadgeText: TextView = view.findViewById(R.id.violationBadge)
        val reportBadgeText: TextView = view.findViewById(R.id.reportBadge)
        val violationBadgeContainer: View = view.findViewById(R.id.violationBadgeContainer)
        val reportBadgeContainer: View = view.findViewById(R.id.reportBadgeContainer)
        val dismissButton: View = view.findViewById(R.id.dismissButton)
        val deleteButton: View = view.findViewById(R.id.deleteButton)
        
        // Loading indicator views
        val dismissIcon: ImageView = view.findViewById(R.id.dismissIcon)
        val dismissText: TextView = view.findViewById(R.id.dismissText)
        val dismissLoadingIndicator: ProgressBar = view.findViewById(R.id.dismissLoadingIndicator)
        val deleteIcon: ImageView = view.findViewById(R.id.deleteIcon)
        val deleteText: TextView = view.findViewById(R.id.deleteText)
        val deleteLoadingIndicator: ProgressBar = view.findViewById(R.id.deleteLoadingIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reported_post_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        val isLoading = post.id in loadingPostIds
        bindViewHolder(holder, post, isLoading)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_LOADING_STATE)) {
            val isLoading = posts[position].id in loadingPostIds
            updateLoadingState(holder, isLoading)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun bindViewHolder(holder: ViewHolder, post: ReportedPost, isLoading: Boolean) {
        holder.titleText.text = post.title
        holder.authorText.text = holder.itemView.context.getString(R.string.by_author, post.author)
        holder.dateText.text = post.date
        holder.categoriesText.text = post.categories.joinToString(" › ")
        holder.descriptionText.text = post.description

        val resources = holder.itemView.context.resources
        val violationText = resources.getQuantityString(
            R.plurals.violation_count, 
            post.violationCount, 
            post.violationCount
        )
        holder.violationBadgeText.text = violationText

        val reportText = resources.getQuantityString(
            R.plurals.report_count, 
            post.reportCount, 
            post.reportCount
        )
        holder.reportBadgeText.text = reportText

        holder.itemView.setOnClickListener {
            onItemClick(post)
        }

        holder.violationBadgeContainer.setOnClickListener {
            onViolationBadgeClick(post)
        }
        
        holder.reportBadgeContainer.setOnClickListener {
            onReportBadgeClick(post)
        }

        holder.dismissButton.setOnClickListener {
            if (!isButtonLoading(holder)) {
                onDismissClick(post)
            }
        }
        
        holder.deleteButton.setOnClickListener {
            if (!isButtonLoading(holder)) {
                onDeleteClick(post)
            }
        }

        updateLoadingState(holder, isLoading)
    }

    private fun updateLoadingState(holder: ViewHolder, isLoading: Boolean) {
        holder.dismissLoadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        holder.dismissIcon.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        holder.dismissText.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        holder.dismissButton.isEnabled = !isLoading
        holder.dismissButton.alpha = if (isLoading) 0.6f else 1.0f

        holder.deleteLoadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        holder.deleteIcon.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        holder.deleteText.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        holder.deleteButton.isEnabled = !isLoading
        holder.deleteButton.alpha = if (isLoading) 0.6f else 1.0f
    }

    private fun isButtonLoading(holder: ViewHolder): Boolean {
        return holder.dismissLoadingIndicator.visibility == View.VISIBLE
    }

    override fun getItemCount() = posts.size

    fun updatePosts(newPosts: List<ReportedPost>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    companion object {
        private const val PAYLOAD_LOADING_STATE = "loading_state"
    }
}
