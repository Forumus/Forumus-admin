package com.anhkhoa.forumus_admin.ui.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.anhkhoa.forumus_admin.MainActivity
import com.anhkhoa.forumus_admin.R
import com.anhkhoa.forumus_admin.data.repository.PostRepository
import com.anhkhoa.forumus_admin.data.repository.UserRepository
import com.anhkhoa.forumus_admin.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

data class TopicData(
    val name: String,
    val percentage: Float,
    val color: Int
)

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val userRepository = UserRepository()
    private val postRepository = PostRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupStatCards()
        setupBarChart()
        setupPieChart()
        setupButtonListeners()
        loadDashboardData()
    }

    private fun setupStatCards() {
        // Total Users Card
        setupStatCard(
            binding.statCardUsers.root,
            R.drawable.ic_total_users,
            R.color.primary_blue,
            getString(R.string.total_users),
            "..."
        )
        
        // Add click listener to navigate to total users screen
        binding.statCardUsers.root.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_totalUsersFragment)
        }

        // Total Posts Card
        setupStatCard(
            binding.statCardPosts.root,
            R.drawable.ic_total_posts,
            R.color.success_green,
            getString(R.string.total_posts),
            "..."
        )
        
        // Add click listener to navigate to total posts screen
        binding.statCardPosts.root.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_totalPostsFragment)
        }

        // Blacklisted Users Card
        setupStatCard(
            binding.statCardBlacklisted.root,
            R.drawable.ic_blacklist,
            R.color.danger_red,
            getString(R.string.blacklisted_users),
            "..."
        )
        
        // Add click listener to navigate to blacklist screen
        binding.statCardBlacklisted.root.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_blacklistFragment)
        }

        // Reported Posts Card
        setupStatCard(
            binding.statCardReported.root,
            R.drawable.ic_reported_posts,
            R.color.warning_orange,
            getString(R.string.reported_posts),
            "..."
        )
        
        // Add click listener to navigate to reported posts screen
        binding.statCardReported.root.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_reportedPostsFragment)
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                // Load users data
                val usersResult = userRepository.getAllUsers()
                usersResult.onSuccess { allUsers ->
                    val totalUsers = allUsers.size
                    val blacklistedUsers = allUsers.count { user ->
                        val status = user.status.lowercase()
                        status == "ban" || status == "warning" || status == "remind"
                    }
                    
                    updateStatCard(binding.statCardUsers.root, formatNumber(totalUsers))
                    updateStatCard(binding.statCardBlacklisted.root, blacklistedUsers.toString())
                }
                
                // Load posts data
                val postsResult = postRepository.getAllPosts()
                postsResult.onSuccess { allPosts ->
                    val totalPosts = allPosts.size
                    val reportedPosts = allPosts.count { it.report_count > 0 }
                    
                    updateStatCard(binding.statCardPosts.root, formatNumber(totalPosts))
                    updateStatCard(binding.statCardReported.root, reportedPosts.toString())
                }
            } catch (e: Exception) {
                // Keep loading indicators if error occurs
            }
        }
    }
    
    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000 -> String.format("%,d", number)
            else -> number.toString()
        }
    }
    
    private fun updateStatCard(cardView: View, value: String) {
        val valueText = cardView.findViewById<TextView>(R.id.valueText)
        valueText.text = value
    }

    private fun setupStatCard(
        cardView: View,
        iconRes: Int,
        colorRes: Int,
        label: String,
        value: String
    ) {
        val iconContainer = cardView.findViewById<FrameLayout>(R.id.iconContainer)
        val iconImage = cardView.findViewById<ImageView>(R.id.iconImage)
        val labelText = cardView.findViewById<TextView>(R.id.labelText)
        val valueText = cardView.findViewById<TextView>(R.id.valueText)

        // Set icon
        iconImage.setImageResource(iconRes)
        
        // Set background color while maintaining rounded corners
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(ContextCompat.getColor(requireContext(), colorRes))
            cornerRadius = resources.getDimension(R.dimen.corner_radius_normal)
        }
        iconContainer.background = drawable
        
        // Set text values
        labelText.text = label
        valueText.text = value
    }

    private fun setupBarChart() {
        val barChart = binding.barChart
        
        // Sample data for weekly posts
        val entries = listOf(
            BarEntry(0f, 350f),
            BarEntry(1f, 280f),
            BarEntry(2f, 320f),
            BarEntry(3f, 180f)
        )

        val dataSet = BarDataSet(entries, "Posts").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_blue)
            valueTextColor = Color.TRANSPARENT
            setDrawValues(false)
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.4f
        }

        barChart.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawBorders(false)
            animateY(1000)
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                valueFormatter = IndexAxisValueFormatter(listOf("Week 1", "Week 2", "Week 3", "Week 4"))
            }
            
            // Configure left Y axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.border_gray)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                axisMinimum = 0f
                granularity = 100f
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            
            // Configure legend
            legend.isEnabled = false
            
            invalidate()
        }
    }

    private fun setupPieChart() {
        // Sample data - Replace this with data from your database
        val topicsData = getSampleTopicData()
        updatePieChart(topicsData)
    }
    
    // Call this method to reload data from database
    fun updatePieChart(topicsData: List<TopicData>) {
        val pieChart = binding.pieChart
        
        // Prepare entries and colors from data
        val entries = topicsData.map { PieEntry(it.percentage, it.name) }
        val colors = topicsData.map { it.color }

        val dataSet = PieDataSet(entries, "Topics").apply {
            setColors(colors)
            sliceSpace = 2f
            setDrawValues(true)
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            }
        }

        val pieData = PieData(dataSet)

        pieChart.apply {
            data = pieData
            description.isEnabled = false
            setDrawEntryLabels(false)
            setUsePercentValues(false)
            holeRadius = 0f
            transparentCircleRadius = 0f
            animateY(1000)
            
            // Disable built-in legend - we'll create custom legend below
            legend.isEnabled = false
            
            invalidate()
        }
        
        // Update custom legends
        updateCustomLegends(topicsData)
    }
    
    private fun updateCustomLegends(topicsData: List<TopicData>) {
        val legendContainer = binding.customLegendContainer
        legendContainer.removeAllViews()
        
        topicsData.forEach { topic ->
            val legendView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_legend, legendContainer, false)
            
            val colorIndicator = legendView.findViewById<View>(R.id.legendColorIndicator)
            val labelText = legendView.findViewById<TextView>(R.id.legendLabel)
            
            // Set the color using GradientDrawable to ensure proper coloring
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(topic.color)
            }
            colorIndicator.background = drawable
            
            labelText.text = topic.name
            
            legendContainer.addView(legendView)
        }
    }
    
    // Sample data method - Replace with your database query
    private fun getSampleTopicData(): List<TopicData> {
        return listOf(
            TopicData(
                getString(R.string.education),
                30f,
                ContextCompat.getColor(requireContext(), R.color.chart_blue)
            ),
            TopicData(
                getString(R.string.entertainment),
                25f,
                ContextCompat.getColor(requireContext(), R.color.chart_red)
            ),
            TopicData(
                getString(R.string.others),
                15f,
                ContextCompat.getColor(requireContext(), R.color.chart_gray)
            ),
            TopicData(
                getString(R.string.sports),
                15f,
                ContextCompat.getColor(requireContext(), R.color.chart_brown)
            ),
            TopicData(
                getString(R.string.technology),
                15f,
                ContextCompat.getColor(requireContext(), R.color.chart_tech_blue)
            )
        )
    }

    private fun setupButtonListeners() {
        // Menu icon to open drawer
        binding.menuIcon.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
        
        // Time range toggle buttons (now TextViews)
        binding.btnDay.setOnClickListener {
            updateToggleSelection(binding.btnDay)
            updateBarChartData("day")
        }
        
        binding.btnWeek.setOnClickListener {
            updateToggleSelection(binding.btnWeek)
            updateBarChartData("week")
        }
        
        binding.btnMonth.setOnClickListener {
            updateToggleSelection(binding.btnMonth)
            updateBarChartData("month")
        }
        
        // Manage topics button
        binding.btnManageTopics.setOnClickListener {
            // Navigate to manage topics screen
            // TODO: Implement navigation
        }
    }
    
    private fun updateToggleSelection(selectedButton: View) {
        // Reset all buttons to unselected state
        binding.btnDay.setBackgroundResource(R.drawable.bg_toggle_button)
        binding.btnWeek.setBackgroundResource(R.drawable.bg_toggle_button)
        binding.btnMonth.setBackgroundResource(R.drawable.bg_toggle_button)
        
        // Set selected button
        selectedButton.setBackgroundResource(R.drawable.bg_toggle_button_selected)
    }

    private fun updateBarChartData(period: String) {
        // TODO: Fetch data based on selected period and update chart
        // This is a placeholder - you can implement actual data fetching logic here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
