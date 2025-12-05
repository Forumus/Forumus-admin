package com.hcmus.forumus_admin.ui.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hcmus.forumus_admin.MainActivity
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.repository.FirestorePost
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
import java.text.SimpleDateFormat
import java.util.*

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
    
    // Predefined colors for topics - distinct colors for better visibility
    private val topicColors = listOf(
        R.color.chart_tech_blue,    // Bright blue
        R.color.chart_red,          // Red
        R.color.chart_green,        // Green
        R.color.chart_orange,       // Orange
        R.color.chart_purple,       // Purple
        R.color.chart_teal,         // Teal
        R.color.chart_pink,         // Pink
        R.color.chart_indigo,       // Indigo
        R.color.chart_amber,        // Amber/Yellow
        R.color.chart_cyan,         // Cyan
        R.color.chart_lime,         // Lime
        R.color.chart_deep_orange,  // Deep Orange
        R.color.chart_brown,        // Brown
        R.color.chart_blue_gray,    // Blue Gray
        R.color.chart_gray          // Gray (last resort)
    )
    
    // Cache for topics loaded from Firebase (used in manage dialog)
    private var cachedFirebaseTopics: List<com.hcmus.forumus_admin.data.repository.FirestoreTopic> = emptyList()
    
    // Cache for posts loaded from Firebase (used in charts)
    private var cachedPosts: List<FirestorePost> = emptyList()
    
    // Current chart period
    private var currentChartPeriod = "week"

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
        setupPostsOverTimeChart()
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

    private fun setupPostsOverTimeChart() {
        // Load posts from Firebase and setup chart
        lifecycleScope.launch {
            try {
                val result = postRepository.getAllPosts()
                result.onSuccess { posts ->
                    cachedPosts = posts
                    updateChartWithPeriod(currentChartPeriod)
                }.onFailure {
                    // Use empty data if Firebase fails
                    updateChartWithPeriod(currentChartPeriod)
                }
            } catch (e: Exception) {
                updateChartWithPeriod(currentChartPeriod)
            }
        }
    }
    
    private fun updateChartWithPeriod(period: String) {
        currentChartPeriod = period
        when (period) {
            "day" -> {
                binding.lineChart.visibility = View.VISIBLE
                binding.barChart.visibility = View.GONE
                binding.tvChartTitle.text = getString(R.string.chart_title_day)
                setupLineChartForDay()
            }
            "week" -> {
                binding.lineChart.visibility = View.GONE
                binding.barChart.visibility = View.VISIBLE
                binding.tvChartTitle.text = getString(R.string.chart_title_week)
                setupBarChartForWeek()
            }
            "month" -> {
                binding.lineChart.visibility = View.GONE
                binding.barChart.visibility = View.VISIBLE
                binding.tvChartTitle.text = getString(R.string.chart_title_month)
                setupBarChartForMonth()
            }
        }
    }
    
    private fun setupLineChartForDay() {
        val lineChart = binding.lineChart
        
        // Get posts from last 24 hours grouped by hour
        val calendar = Calendar.getInstance()
        val now = calendar.time
        calendar.add(Calendar.HOUR_OF_DAY, -24)
        val startTime = calendar.time
        
        // Initialize hourly counts (0-23)
        val hourlyCounts = IntArray(24) { 0 }
        val hourLabels = mutableListOf<String>()
        
        // Generate hour labels
        val hourFormat = SimpleDateFormat("HH:00", Locale.getDefault())
        calendar.time = startTime
        for (i in 0 until 24) {
            hourLabels.add(hourFormat.format(calendar.time))
            calendar.add(Calendar.HOUR_OF_DAY, 1)
        }
        
        // Count posts per hour
        cachedPosts.forEach { post ->
            val postDate = PostRepository.getFirebaseTimestampAsDate(post.createdAt)
            if (postDate != null && postDate.after(startTime) && postDate.before(now)) {
                val postCalendar = Calendar.getInstance()
                postCalendar.time = postDate
                val hoursSinceStart = ((postDate.time - startTime.time) / (1000 * 60 * 60)).toInt()
                if (hoursSinceStart in 0..23) {
                    hourlyCounts[hoursSinceStart]++
                }
            }
        }
        
        // Create line entries
        val entries = hourlyCounts.mapIndexed { index, count ->
            Entry(index.toFloat(), count.toFloat())
        }
        
        val dataSet = LineDataSet(entries, "Posts").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_blue)
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            setDrawCircleHole(true)
            circleHoleRadius = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.primary_blue)
            fillAlpha = 30
        }
        
        val lineData = LineData(dataSet)
        
        lineChart.apply {
            data = lineData
            description.isEnabled = false
            setDrawGridBackground(false)
            animateX(1000)
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 4f
                labelCount = 6
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
                valueFormatter = IndexAxisValueFormatter(hourLabels)
            }
            
            // Configure left Y axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.border_gray)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                axisMinimum = 0f
                granularity = 1f
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            legend.isEnabled = false
            
            invalidate()
        }
    }
    
    private fun setupBarChartForWeek() {
        val barChart = binding.barChart
        
        // Get posts from last 7 days
        val calendar = Calendar.getInstance()
        val now = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.time
        
        // Initialize daily counts
        val dailyCounts = IntArray(7) { 0 }
        val dayLabels = mutableListOf<String>()
        
        // Generate day labels
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        calendar.time = startTime
        for (i in 0 until 7) {
            dayLabels.add(dayFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Count posts per day
        cachedPosts.forEach { post ->
            val postDate = PostRepository.getFirebaseTimestampAsDate(post.createdAt)
            if (postDate != null && postDate.after(startTime) && postDate.before(now)) {
                val daysSinceStart = ((postDate.time - startTime.time) / (1000 * 60 * 60 * 24)).toInt()
                if (daysSinceStart in 0..6) {
                    dailyCounts[daysSinceStart]++
                }
            }
        }
        
        // Create bar entries
        val entries = dailyCounts.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        
        val dataSet = BarDataSet(entries, "Posts").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_blue)
            valueTextColor = Color.TRANSPARENT
            setDrawValues(false)
        }
        
        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
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
                valueFormatter = IndexAxisValueFormatter(dayLabels)
            }
            
            // Configure left Y axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.border_gray)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                axisMinimum = 0f
                granularity = 1f
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            legend.isEnabled = false
            
            invalidate()
        }
    }
    
    private fun setupBarChartForMonth() {
        val barChart = binding.barChart
        
        // Get posts from last 12 months
        val calendar = Calendar.getInstance()
        val now = calendar.time
        calendar.add(Calendar.MONTH, -12)
        val startTime = calendar.time
        
        // Initialize monthly counts
        val monthlyCounts = IntArray(12) { 0 }
        val monthLabels = mutableListOf<String>()
        
        // Generate month labels
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        calendar.time = startTime
        for (i in 0 until 12) {
            monthLabels.add(monthFormat.format(calendar.time))
            calendar.add(Calendar.MONTH, 1)
        }
        
        // Count posts per month
        calendar.time = startTime
        cachedPosts.forEach { post ->
            val postDate = PostRepository.getFirebaseTimestampAsDate(post.createdAt)
            if (postDate != null && postDate.after(startTime) && postDate.before(now)) {
                val postCalendar = Calendar.getInstance()
                postCalendar.time = postDate
                
                val startCalendar = Calendar.getInstance()
                startCalendar.time = startTime
                
                // Calculate month difference
                val yearDiff = postCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR)
                val monthDiff = postCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH)
                val monthIndex = yearDiff * 12 + monthDiff
                
                if (monthIndex in 0..11) {
                    monthlyCounts[monthIndex]++
                }
            }
        }
        
        // Create bar entries
        val entries = monthlyCounts.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        
        val dataSet = BarDataSet(entries, "Posts").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_blue)
            valueTextColor = Color.TRANSPARENT
            setDrawValues(false)
        }
        
        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
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
                labelCount = 12
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 9f
                valueFormatter = IndexAxisValueFormatter(monthLabels)
            }
            
            // Configure left Y axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.border_gray)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                axisMinimum = 0f
                granularity = 1f
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
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
                    // Cache topics for use in manage dialog
                    cachedFirebaseTopics = topics
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
            updateChartWithPeriod("day")
        }
        
        binding.btnWeek.setOnClickListener {
            updateToggleSelection(binding.btnWeek)
            updateChartWithPeriod("week")
        }
        
        binding.btnMonth.setOnClickListener {
            updateToggleSelection(binding.btnMonth)
            updateChartWithPeriod("month")
        }
        
        // Manage topics button
        binding.btnManageTopics.setOnClickListener {
            showManageTopicsDialog()
        }
    }
    
    private fun showManageTopicsDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_manage_topics, null)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_rounded))
            .create()
        
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val etNewTopic = dialogView.findViewById<EditText>(R.id.etNewTopic)
        val btnAddTopic = dialogView.findViewById<MaterialButton>(R.id.btnAddTopic)
        val rvTopics = dialogView.findViewById<RecyclerView>(R.id.rvTopics)
        
        // Get current topics from Firebase cache with their colors
        val currentTopics = mutableListOf<ManageTopicItem>()
        
        val adapter = ManageTopicsAdapter(
            onItemClick = { topic ->
                // Show edit dialog when item is clicked
                showEditTopicDialog(topic, currentTopics, dialog)
            },
            onDeleteClick = { topic ->
                // Show confirmation dialog before deleting
                showDeleteTopicConfirmation(topic, currentTopics, dialog)
            }
        )
        
        rvTopics.layoutManager = LinearLayoutManager(requireContext())
        rvTopics.adapter = adapter
        
        // Load topics from Firebase
        loadTopicsForDialog(currentTopics, adapter)
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        btnAddTopic.setOnClickListener {
            val topicName = etNewTopic.text.toString().trim()
            if (topicName.isEmpty()) {
                Toast.makeText(requireContext(), R.string.topic_name_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if topic already exists
            if (currentTopics.any { it.name.equals(topicName, ignoreCase = true) }) {
                Toast.makeText(requireContext(), R.string.topic_already_exists, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Show description dialog
            showTopicDescriptionDialog(topicName, currentTopics, adapter, etNewTopic)
        }
        
        dialog.show()
    }
    
    private fun showTopicDescriptionDialog(
        topicName: String,
        topicsList: MutableList<ManageTopicItem>,
        adapter: ManageTopicsAdapter,
        etNewTopic: EditText
    ) {
        val descDialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_topic_description, null)
        
        val descDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(descDialogView)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_rounded))
            .create()
        
        val tvTopicName = descDialogView.findViewById<TextView>(R.id.tvTopicName)
        val etDescription = descDialogView.findViewById<EditText>(R.id.etDescription)
        val btnCancel = descDialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = descDialogView.findViewById<MaterialButton>(R.id.btnSave)
        
        tvTopicName.text = getString(R.string.topic_name_label, topicName)
        
        btnCancel.setOnClickListener {
            descDialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val description = etDescription.text.toString().trim()
            if (description.isEmpty()) {
                Toast.makeText(requireContext(), R.string.topic_description_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Save to Firebase
            lifecycleScope.launch {
                try {
                    val result = topicRepository.addTopic(topicName, description)
                    result.onSuccess { newFirestoreTopic ->
                        // Add new topic with a color from predefined palette
                        val colorIndex = topicsList.size % topicColors.size
                        val newTopic = ManageTopicItem(
                            id = newFirestoreTopic.id,
                            name = newFirestoreTopic.name,
                            description = newFirestoreTopic.description,
                            color = ContextCompat.getColor(requireContext(), topicColors[colorIndex])
                        )
                        topicsList.add(newTopic)
                        adapter.submitList(topicsList.toList())
                        etNewTopic.text.clear()
                        
                        Toast.makeText(requireContext(), R.string.topic_added, Toast.LENGTH_SHORT).show()
                        
                        // Refresh the pie chart
                        refreshPieChart()
                        descDialog.dismiss()
                    }.onFailure {
                        Toast.makeText(requireContext(), R.string.topic_add_failed, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.topic_add_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        descDialog.show()
    }
    
    private fun loadTopicsForDialog(topicsList: MutableList<ManageTopicItem>, adapter: ManageTopicsAdapter) {
        // Load topics from Firebase
        lifecycleScope.launch {
            try {
                val result = topicRepository.getAllTopics()
                result.onSuccess { topics ->
                    topicsList.clear()
                    topics.forEachIndexed { index, topic ->
                        val colorIndex = index % topicColors.size
                        topicsList.add(
                            ManageTopicItem(
                                id = topic.id,
                                name = topic.name,
                                description = topic.description,
                                color = ContextCompat.getColor(requireContext(), topicColors[colorIndex])
                            )
                        )
                    }
                    adapter.submitList(topicsList.toList())
                }.onFailure {
                    // Show error or use cached data
                    loadCachedTopicsForDialog(topicsList, adapter)
                }
            } catch (e: Exception) {
                loadCachedTopicsForDialog(topicsList, adapter)
            }
        }
    }
    
    private fun loadCachedTopicsForDialog(topicsList: MutableList<ManageTopicItem>, adapter: ManageTopicsAdapter) {
        // Use cached Firebase topics if available
        if (cachedFirebaseTopics.isNotEmpty()) {
            topicsList.clear()
            cachedFirebaseTopics.forEachIndexed { index, topic ->
                val colorIndex = index % topicColors.size
                topicsList.add(
                    ManageTopicItem(
                        id = topic.id,
                        name = topic.name,
                        description = topic.description,
                        color = ContextCompat.getColor(requireContext(), topicColors[colorIndex])
                    )
                )
            }
            adapter.submitList(topicsList.toList())
        }
    }
    
    private fun showEditTopicDialog(
        topic: ManageTopicItem,
        topicsList: MutableList<ManageTopicItem>,
        parentDialog: AlertDialog
    ) {
        val editDialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_topic, null)
        
        val editDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(editDialogView)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_rounded))
            .create()
        
        val btnClose = editDialogView.findViewById<ImageButton>(R.id.btnClose)
        val colorIndicator = editDialogView.findViewById<View>(R.id.topicColorIndicator)
        val etTopicName = editDialogView.findViewById<EditText>(R.id.etTopicName)
        val etTopicDescription = editDialogView.findViewById<EditText>(R.id.etTopicDescription)
        val btnCancel = editDialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = editDialogView.findViewById<MaterialButton>(R.id.btnSave)
        
        // Set current values
        etTopicName.setText(topic.name)
        etTopicDescription.setText(topic.description)
        
        // Set color indicator
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6f * requireContext().resources.displayMetrics.density
            setColor(topic.color)
        }
        colorIndicator.background = drawable
        
        btnClose.setOnClickListener {
            editDialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            editDialog.dismiss()
        }
        
        btnSave.setOnClickListener {
            val newName = etTopicName.text.toString().trim()
            val newDescription = etTopicDescription.text.toString().trim()
            
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), R.string.topic_name_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (newDescription.isEmpty()) {
                Toast.makeText(requireContext(), R.string.topic_description_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if name already exists (excluding current topic)
            if (topicsList.any { it.id != topic.id && it.name.equals(newName, ignoreCase = true) }) {
                Toast.makeText(requireContext(), R.string.topic_already_exists, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Update in Firebase
            lifecycleScope.launch {
                try {
                    val result = topicRepository.updateTopic(topic.id, newName, newDescription)
                    result.onSuccess {
                        // Update local list
                        val index = topicsList.indexOfFirst { it.id == topic.id }
                        if (index != -1) {
                            topicsList[index] = topic.copy(
                                name = newName,
                                description = newDescription
                            )
                            (parentDialog.findViewById<RecyclerView>(R.id.rvTopics)?.adapter as? ManageTopicsAdapter)
                                ?.submitList(topicsList.toList())
                        }
                        
                        Toast.makeText(requireContext(), R.string.topic_updated, Toast.LENGTH_SHORT).show()
                        
                        // Refresh the pie chart
                        refreshPieChart()
                        editDialog.dismiss()
                    }.onFailure {
                        Toast.makeText(requireContext(), R.string.topic_update_failed, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.topic_update_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        editDialog.show()
    }
    
    private fun getRandomTopicColor(): Int {
        val colors = listOf(
            R.color.chart_blue,
            R.color.chart_red,
            R.color.chart_brown,
            R.color.chart_tech_blue,
            R.color.chart_purple,
            R.color.chart_orange,
            R.color.chart_teal,
            R.color.chart_pink
        )
        return ContextCompat.getColor(requireContext(), colors.random())
    }
    
    private fun showDeleteTopicConfirmation(
        topic: ManageTopicItem,
        topicsList: MutableList<ManageTopicItem>,
        parentDialog: AlertDialog
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_topic_title)
            .setMessage(getString(R.string.confirm_delete_topic_message, topic.name))
            .setPositiveButton(R.string.ok) { _, _ ->
                // Delete from Firebase
                lifecycleScope.launch {
                    try {
                        val result = topicRepository.deleteTopic(topic.id)
                        result.onSuccess {
                            topicsList.remove(topic)
                            (parentDialog.findViewById<RecyclerView>(R.id.rvTopics)?.adapter as? ManageTopicsAdapter)
                                ?.submitList(topicsList.toList())
                            
                            Toast.makeText(requireContext(), R.string.topic_deleted, Toast.LENGTH_SHORT).show()
                            
                            // Refresh the pie chart
                            refreshPieChart()
                        }.onFailure {
                            Toast.makeText(requireContext(), R.string.topic_delete_failed, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), R.string.topic_delete_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun refreshPieChart() {
        // Refresh the pie chart with updated topics
        setupPieChart()
    }
    
    private fun updateToggleSelection(selectedButton: View) {
        // Reset all buttons to unselected state
        binding.btnDay.setBackgroundResource(R.drawable.bg_toggle_button)
        binding.btnWeek.setBackgroundResource(R.drawable.bg_toggle_button)
        binding.btnMonth.setBackgroundResource(R.drawable.bg_toggle_button)
        
        // Set selected button
        selectedButton.setBackgroundResource(R.drawable.bg_toggle_button_selected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
