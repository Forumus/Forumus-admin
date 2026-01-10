package com.hcmus.forumus_admin.ui.reported

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reported_post_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        
        holder.titleText.text = post.title
        holder.authorText.text = holder.itemView.context.getString(R.string.by_author, post.author)
        holder.dateText.text = post.date
        
        // Format categories with › separator
        holder.categoriesText.text = post.categories.joinToString(" › ")
        
        holder.descriptionText.text = post.description
        
        // Set violation count
        val resources = holder.itemView.context.resources
        val violationText = resources.getQuantityString(
            R.plurals.violation_count, 
            post.violationCount, 
            post.violationCount
        )
        holder.violationBadgeText.text = violationText
        
        // Set report count
        val reportText = resources.getQuantityString(
            R.plurals.report_count, 
            post.reportCount, 
            post.reportCount
        )
        holder.reportBadgeText.text = reportText
        
        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClick(post)
        }
        
        // Handle badge clicks
        holder.violationBadgeContainer.setOnClickListener {
            onViolationBadgeClick(post)
        }
        
        holder.reportBadgeContainer.setOnClickListener {
            onReportBadgeClick(post)
        }
        
        // Handle button clicks
        holder.dismissButton.setOnClickListener {
            onDismissClick(post)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(post)
        }
    }

    override fun getItemCount() = posts.size

    fun updatePosts(newPosts: List<ReportedPost>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
