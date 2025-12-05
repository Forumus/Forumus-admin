package com.hcmus.forumus_admin.ui.dashboard

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_admin.R

data class ManageTopicItem(
    val id: String,
    val name: String,
    val description: String,
    val color: Int
)

class ManageTopicsAdapter(
    private val onItemClick: (ManageTopicItem) -> Unit,
    private val onDeleteClick: (ManageTopicItem) -> Unit
) : ListAdapter<ManageTopicItem, ManageTopicsAdapter.TopicViewHolder>(TopicDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator: View = itemView.findViewById(R.id.topicColorIndicator)
        private val tvTopicName: TextView = itemView.findViewById(R.id.tvTopicName)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTopic)

        fun bind(topic: ManageTopicItem) {
            tvTopicName.text = topic.name
            
            // Set the color indicator
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6f * itemView.context.resources.displayMetrics.density
                setColor(topic.color)
            }
            colorIndicator.background = drawable

            // Item click to view/edit topic details
            itemView.setOnClickListener {
                onItemClick(topic)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(topic)
            }
        }
    }

    private class TopicDiffCallback : DiffUtil.ItemCallback<ManageTopicItem>() {
        override fun areItemsTheSame(oldItem: ManageTopicItem, newItem: ManageTopicItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ManageTopicItem, newItem: ManageTopicItem): Boolean {
            return oldItem == newItem
        }
    }
}
