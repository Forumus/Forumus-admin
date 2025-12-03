package com.hcmus.forumus_admin.ui.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hcmus.forumus_admin.MainActivity
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.repository.PostRepository
import com.hcmus.forumus_admin.data.repository.TopicRepository
import com.hcmus.forumus_admin.data.repository.UserRepository
import com.hcmus.forumus_admin.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

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
    private val topicRepository = TopicRepository()
    
    // Threshold for grouping small topics into "Others" (3%)
    private val othersThreshold = 3f
    
    // Predefined colors for topics
    private val topicColors = listOf(
        R.color.chart_blue,
        R.color.chart_red,
        R.color.chart_brown,
        R.color.chart_tech_blue,
        R.color.chart_purple,
        R.color.chart_orange,
        R.color.chart_teal,
        R.color.chart_pink
    )

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
        // Load real data from Firebase
        loadTopicsData()
    }
    
    private fun loadTopicsData() {
        lifecycleScope.launch {
            try {
                val topicsResult = topicRepository.getAllTopics()
                topicsResult.onSuccess { topics ->
                    val topicsData = processTopicsData(topics)
                    updatePieChart(topicsData)
                }.onFailure {
                    // Fallback to sample data if Firebase fails
                    updatePieChart(getSampleTopicData())
                }
            } catch (e: Exception) {
                // Fallback to sample data if error occurs
                updatePieChart(getSampleTopicData())
            }
        }
    }
    
    private fun processTopicsData(topics: List<com.hcmus.forumus_admin.data.repository.FirestoreTopic>): List<TopicData> {
        // Calculate total posts across all topics
        val totalPosts = topics.sumOf { it.postCount }
        
        if (totalPosts == 0) {
            return getSampleTopicData()
        }
        
        // Calculate percentage for each topic
        val topicsWithPercentage = topics.map { topic ->
            val percentage = (topic.postCount.toFloat() / totalPosts) * 100f
            Triple(topic.name, percentage, topic.postCount)
        }.sortedByDescending { it.second }
        
        // Separate topics above and below threshold
        val mainTopics = mutableListOf<TopicData>()
        var othersPercentage = 0f
        var colorIndex = 0
        
        for ((name, percentage, _) in topicsWithPercentage) {
            if (percentage >= othersThreshold) {
                // Add topic with assigned color
                val colorRes = topicColors.getOrElse(colorIndex) { R.color.chart_gray }
                mainTopics.add(
                    TopicData(
                        name = name,
                        percentage = percentage,
                        color = ContextCompat.getColor(requireContext(), colorRes)
                    )
                )
                colorIndex++
            } else {
                // Accumulate into "Others"
                othersPercentage += percentage
            }
        }
        
        // Add "Others" category if there are small topics
        if (othersPercentage > 0f) {
            mainTopics.add(
                TopicData(
                    name = getString(R.string.others),
                    percentage = othersPercentage,
                    color = ContextCompat.getColor(requireContext(), R.color.chart_gray)
                )
            )
        }
        
        return mainTopics
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
            // Hide all percentage labels by default
            setDrawValues(false)
            valueTextSize = 10f
            valueTextColor = Color.WHITE
            // Position values in outer area of segment for better readability
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
            // Set selection shift for expanding effect when clicked
            selectionShift = 12f
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
            
            // Add click listener for segment selection
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e != null && h != null) {
                        // Show percentage only for selected segment
                        dataSet.setDrawValues(true)
                        dataSet.valueFormatter = object : ValueFormatter() {
                            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                                // Only show value for the selected entry
                                return if (pieEntry == e) "${value.toInt()}%" else ""
                            }
                        }
                        invalidate()
                    }
                }

                override fun onNothingSelected() {
                    // Hide all values when nothing is selected
                    dataSet.setDrawValues(false)
                    invalidate()
                }
            })
            
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
    
    // Fallback sample data when Firebase fails
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
