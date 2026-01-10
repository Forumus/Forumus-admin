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
import com.hcmus.forumus_admin.data.cache.DashboardCacheManager
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
    
    // Cache manager for efficient data loading
    private lateinit var cacheManager: DashboardCacheManager
    
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
    
    // Navigation state for charts
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var currentWeekStart: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }

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
        
        // Initialize cache manager
        cacheManager = DashboardCacheManager.getInstance(requireContext())
        
        setupStatCards()
        setupSwipeRefresh()
        setupPostsOverTimeChart()
        setupPieChart()
        setupButtonListeners()
        loadDashboardData()
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(
                R.color.primary_blue,
                R.color.success_green,
                R.color.warning_orange
            )
            setOnRefreshListener {
                // Invalidate cache and refresh data
                cacheManager.invalidateCache()
                refreshAllData()
            }
        }
    }
    
    private fun refreshAllData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Refresh stat cards
                loadDashboardDataFromNetwork(forceRefresh = true)
                
                // Refresh charts
                loadPostsDataFromNetwork(forceRefresh = true)
                loadTopicsDataFromNetwork(forceRefresh = true)
            } finally {
                _binding?.swipeRefreshLayout?.isRefreshing = false
            }
        }
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
        // Try to load from cache first if valid
        if (cacheManager.isCacheValid()) {
            val cachedStats = cacheManager.getDashboardStats()
            if (cachedStats != null) {
                // Update UI with cached data
                updateStatCard(binding.statCardUsers.root, formatNumber(cachedStats.totalUsers))
                updateStatCard(binding.statCardBlacklisted.root, cachedStats.blacklistedUsers.toString())
                updateStatCard(binding.statCardPosts.root, formatNumber(cachedStats.totalPosts))
                updateStatCard(binding.statCardReported.root, cachedStats.reportedPosts.toString())
                return
            }
        }
        
        // Cache is invalid or doesn't exist, load from network
        loadDashboardDataFromNetwork(forceRefresh = false)
    }
    
    private fun loadDashboardDataFromNetwork(forceRefresh: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var totalUsers = 0
                var blacklistedUsers = 0
                var totalPosts = 0
                var reportedPosts = 0
                
                // Check if fragment is still added
                if (!isAdded || _binding == null) return@launch
                
                // Load users data
                val usersResult = userRepository.getAllUsers()
                usersResult.onSuccess { allUsers ->
                    totalUsers = allUsers.size
                    blacklistedUsers = allUsers.count { user ->
                        val status = user.status.lowercase()
                        status == "banned" || status == "warned" || status == "reminded"
                    }
                    
                    _binding?.let { binding ->
                        updateStatCard(binding.statCardUsers.root, formatNumber(totalUsers))
                        updateStatCard(binding.statCardBlacklisted.root, blacklistedUsers.toString())
                    }
                }
                
                // Load posts data
                val postsResult = postRepository.getAllPosts()
                postsResult.onSuccess { allPosts ->
                    totalPosts = allPosts.size
                    reportedPosts = allPosts.count { it.reportCount > 0 }
                    
                    _binding?.let { binding ->
                        updateStatCard(binding.statCardPosts.root, formatNumber(totalPosts))
                        updateStatCard(binding.statCardReported.root, reportedPosts.toString())
                    }
                }
                
                // Save to cache
                cacheManager.saveDashboardStats(
                    DashboardCacheManager.DashboardStats(
                        totalUsers = totalUsers,
                        blacklistedUsers = blacklistedUsers,
                        totalPosts = totalPosts,
                        reportedPosts = reportedPosts
                    )
                )
            } catch (e: Exception) {
                // Keep loading indicators if error occurs
            }
        }
    }
    
    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000 -> "%,d".format(number)
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
        // Setup navigation buttons
        binding.btnPrevPeriod.setOnClickListener { navigateToPreviousPeriod() }
        binding.btnNextPeriod.setOnClickListener { navigateToNextPeriod() }
        
        // Try to load from cache first
        if (cacheManager.isCacheValid()) {
            val cachedPosts = cacheManager.getPostsData()
            if (cachedPosts != null) {
                this.cachedPosts = cachedPosts
                updateChartWithPeriod(currentChartPeriod)
                return
            }
        }
        
        // Load from network
        loadPostsDataFromNetwork(forceRefresh = false)
    }
    
    private fun loadPostsDataFromNetwork(forceRefresh: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Check if fragment is still added
                if (!isAdded || _binding == null) return@launch
                
                val result = postRepository.getAllPosts()
                result.onSuccess { posts ->
                    cachedPosts = posts
                    // Save to cache
                    cacheManager.savePostsData(posts)
                    if (isAdded && _binding != null) {
                        updateChartWithPeriod(currentChartPeriod)
                    }
                }.onFailure {
                    // Use empty data if Firebase fails
                    if (isAdded && _binding != null) {
                        updateChartWithPeriod(currentChartPeriod)
                    }
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    updateChartWithPeriod(currentChartPeriod)
                }
            }
        }
    }
    
    private fun navigateToPreviousPeriod() {
        when (currentChartPeriod) {
            "month" -> {
                currentYear--
            }
            "week" -> {
                if (currentMonth == 0) {
                    currentMonth = 11
                    currentYear--
                } else {
                    currentMonth--
                }
            }
            "day" -> {
                currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            }
        }
        updateChartWithPeriod(currentChartPeriod)
    }
    
    private fun navigateToNextPeriod() {
        when (currentChartPeriod) {
            "month" -> {
                currentYear++
            }
            "week" -> {
                if (currentMonth == 11) {
                    currentMonth = 0
                    currentYear++
                } else {
                    currentMonth++
                }
            }
            "day" -> {
                currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        updateChartWithPeriod(currentChartPeriod)
    }
    
    private fun updatePeriodLabel() {
        val monthNames = arrayOf(
            getString(R.string.jan), getString(R.string.feb), getString(R.string.mar), 
            getString(R.string.apr), getString(R.string.may), getString(R.string.jun), 
            getString(R.string.jul), getString(R.string.aug), getString(R.string.sep), 
            getString(R.string.oct), getString(R.string.nov), getString(R.string.dec)
        )
        val label = when (currentChartPeriod) {
            "month" -> currentYear.toString()
            "week" -> {
                if (Locale.getDefault().language == "vi") {
                   	"${monthNames[currentMonth]} $currentYear"
                } else {
                    "${monthNames[currentMonth]} $currentYear"
                }
            }
            "day" -> {
                val weekEnd = currentWeekStart.clone() as Calendar
                weekEnd.add(Calendar.DAY_OF_WEEK, 6)
                
                // Use localized formatted string for the date range
                val startFormat = SimpleDateFormat(getString(R.string.chart_day_format), Locale.getDefault())
                val endFormat = SimpleDateFormat(getString(R.string.chart_day_format), Locale.getDefault())
                
                // Format: "Nov 17 - Nov 23, 2024" or equivalent
                "${startFormat.format(currentWeekStart.time)} - ${endFormat.format(weekEnd.time)}, ${currentWeekStart.get(Calendar.YEAR)}"
            }
            else -> ""
        }
        binding.tvCurrentPeriod.text = label
    }
    
    private fun updateChartWithPeriod(period: String) {
        currentChartPeriod = period
        updatePeriodLabel()
        
        when (period) {
            "day" -> {
                binding.tvChartTitle.text = getString(R.string.chart_title_day)
                setupBarChartForDay()
            }
            "week" -> {
                binding.tvChartTitle.text = getString(R.string.chart_title_week)
                setupBarChartForWeek()
            }
            "month" -> {
                binding.tvChartTitle.text = getString(R.string.chart_title_month)
                setupBarChartForMonth()
            }
        }
    }
    
    private fun setupBarChartForDay() {
        val barChart = binding.barChart
        
        // Get the week start and end
        val weekStart = currentWeekStart.clone() as Calendar
        weekStart.set(Calendar.HOUR_OF_DAY, 0)
        weekStart.set(Calendar.MINUTE, 0)
        weekStart.set(Calendar.SECOND, 0)
        weekStart.set(Calendar.MILLISECOND, 0)
        
        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 7)
        
        // Initialize daily counts for 7 days of the week
        val dailyCounts = IntArray(7) { 0 }
        val dayLabels = mutableListOf<String>()
        
        // Generate day labels using localized strings
        // We order them based on the calendar (e.g. starting Monday or Sunday)
        // Instead of hardcoded array, we can use format 
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        // BUT user provided resources: mon, tue etc. Let's try to map if we can, 
        // OR rely on SimpleDateFormat("EEE", Locale.getDefault()) which is standard.
        // Given user provided "Th 2", "Th 3" etc for Vietnam, standard EEE might give "Th 2" or "Mon".
        // Let's use the resources to be safe and match user expectation
        
        val tempCal = weekStart.clone() as Calendar
        // Map calendar day constant to resource string
        fun getDayString(cal: Calendar): String {
            return when(cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> getString(R.string.mon)
                Calendar.TUESDAY -> getString(R.string.tue)
                Calendar.WEDNESDAY -> getString(R.string.wed)
                Calendar.THURSDAY -> getString(R.string.thu)
                Calendar.FRIDAY -> getString(R.string.fri)
                Calendar.SATURDAY -> getString(R.string.sat)
                Calendar.SUNDAY -> getString(R.string.sun)
                else -> ""
            }
        }

        for (i in 0 until 7) {
            dayLabels.add(getDayString(tempCal))
            tempCal.add(Calendar.DAY_OF_WEEK, 1)
        }
        
        // Count posts per day in this week
        cachedPosts.forEach { post ->
            val postDate = PostRepository.getFirebaseTimestampAsDate(post.createdAt)
            if (postDate != null && !postDate.before(weekStart.time) && postDate.before(weekEnd.time)) {
                val postCalendar = Calendar.getInstance()
                postCalendar.time = postDate
                
                val daysSinceStart = ((postDate.time - weekStart.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                if (daysSinceStart in 0..6) {
                    dailyCounts[daysSinceStart]++
                }
            }
        }
        
        // Create bar entries
        val entries = dailyCounts.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        
        val dataSet = BarDataSet(entries, getString(R.string.total_posts)).apply {
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
            
            // Disable zooming and scaling for immediate proper view
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 7
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
            
            // Reset zoom and fit data
            setVisibleXRangeMaximum(7f)
            moveViewToX(0f)
            
            invalidate()
        }
    }
    
    private fun setupBarChartForWeek() {
        val barChart = binding.barChart
        
        // Get weeks in the selected month
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val monthStart = calendar.time
        
        // Get the number of weeks in this month
        val maxWeek = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH)
        
        // Move to next month for end boundary
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.time
        
        // Initialize weekly counts
        val weeklyCounts = IntArray(maxWeek) { 0 }
        
        // Use localized label for "Week X"
        val weekLabels = (1..maxWeek).map { getString(R.string.chart_week_label).format(it) }
        
        // Count posts per week in this month
        cachedPosts.forEach { post ->
            val postDate = PostRepository.getFirebaseTimestampAsDate(post.createdAt)
            if (postDate != null && !postDate.before(monthStart) && postDate.before(monthEnd)) {
                val postCalendar = Calendar.getInstance()
                postCalendar.time = postDate
                
                // Get week of month (1-based)
                val weekOfMonth = postCalendar.get(Calendar.WEEK_OF_MONTH) - 1
                if (weekOfMonth in 0 until maxWeek) {
                    weeklyCounts[weekOfMonth]++
                }
            }
        }
        
        // Create bar entries
        val entries = weeklyCounts.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        
        val dataSet = BarDataSet(entries, getString(R.string.total_posts)).apply {
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
            
            // Disable zooming and scaling for immediate proper view
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = maxWeek
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                valueFormatter = IndexAxisValueFormatter(weekLabels)
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
            
            // Reset zoom and fit data
            setVisibleXRangeMaximum(maxWeek.toFloat())
            moveViewToX(0f)
            
            invalidate()
        }
    }
    
    private fun setupBarChartForMonth() {
        val barChart = binding.barChart
        
        // Get all 12 months in the selected year
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, 0) // January
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val yearStart = calendar.time
        
        // Move to next year for end boundary
        calendar.add(Calendar.YEAR, 1)
        val yearEnd = calendar.time
        
        // Initialize monthly counts
        val monthlyCounts = IntArray(12) { 0 }
        
        // Use localized month names
        val monthLabels = listOf(
            getString(R.string.jan), getString(R.string.feb), getString(R.string.mar), 
            getString(R.string.apr), getString(R.string.may), getString(R.string.jun), 
            getString(R.string.jul), getString(R.string.aug), getString(R.string.sep), 
            getString(R.string.oct), getString(R.string.nov), getString(R.string.dec)
        )
        
        // Count posts per month in this year
        cachedPosts.forEach { post ->
            val postDate = PostRepository.getFirebaseTimestampAsDate(post.createdAt)
            if (postDate != null && !postDate.before(yearStart) && postDate.before(yearEnd)) {
                val postCalendar = Calendar.getInstance()
                postCalendar.time = postDate
                
                val monthIndex = postCalendar.get(Calendar.MONTH)
                if (monthIndex in 0..11) {
                    monthlyCounts[monthIndex]++
                }
            }
        }
        
        // Create bar entries
        val entries = monthlyCounts.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }
        
        val dataSet = BarDataSet(entries, getString(R.string.total_posts)).apply {
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
            
            // Disable zooming and scaling for immediate proper view
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            
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
            
            // Reset zoom and fit data
            setVisibleXRangeMaximum(12f)
            moveViewToX(0f)
            
            invalidate()
        }
    }

    private fun setupPieChart() {
        // Try to load from cache first
        if (cacheManager.isCacheValid()) {
            val cachedTopics = cacheManager.getTopicsData()
            if (cachedTopics != null) {
                cachedFirebaseTopics = cachedTopics
                val topicsData = processTopicsData(cachedTopics)
                updatePieChart(topicsData)
                return
            }
        }
        
        // Load from network
        loadTopicsDataFromNetwork(forceRefresh = false)
    }
    
    private fun loadTopicsDataFromNetwork(forceRefresh: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Check if fragment is still added
                if (!isAdded || _binding == null) return@launch
                
                val topicsResult = topicRepository.getAllTopics()
                topicsResult.onSuccess { topics ->
                    // Cache topics for use in manage dialog
                    cachedFirebaseTopics = topics
                    // Save to cache
                    cacheManager.saveTopicsData(topics)
                    if (isAdded && _binding != null) {
                        val topicsData = processTopicsData(topics)
                        updatePieChart(topicsData)
                    }
                }.onFailure {
                    // Fallback to sample data if Firebase fails
                    if (isAdded && _binding != null) {
                        updatePieChart(getSampleTopicData())
                    }
                }
            } catch (e: Exception) {
                // Fallback to sample data if error occurs
                if (isAdded && _binding != null) {
                    updatePieChart(getSampleTopicData())
                }
            }
        }
    }
    
    private fun loadTopicsData() {
        loadTopicsDataFromNetwork(forceRefresh = false)
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
                context?.let { Toast.makeText(it, R.string.topic_name_empty, Toast.LENGTH_SHORT).show() }
                return@setOnClickListener
            }
            
            // Check if topic already exists
            if (currentTopics.any { it.name.equals(topicName, ignoreCase = true) }) {
                context?.let { Toast.makeText(it, R.string.topic_already_exists, Toast.LENGTH_SHORT).show() }
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
                context?.let { Toast.makeText(it, R.string.topic_description_empty, Toast.LENGTH_SHORT).show() }
                return@setOnClickListener
            }
            
            // Save to Firebase
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (!isAdded) return@launch
                    
                    val result = topicRepository.addTopic(topicName, description)
                    result.onSuccess { newFirestoreTopic ->
                        // Add new topic with a color from predefined palette
                        val colorIndex = topicsList.size % topicColors.size
                        context?.let { ctx ->
                            val newTopic = ManageTopicItem(
                                id = newFirestoreTopic.id,
                                name = newFirestoreTopic.name,
                                description = newFirestoreTopic.description,
                                color = ContextCompat.getColor(ctx, topicColors[colorIndex])
                            )
                            topicsList.add(newTopic)
                            adapter.submitList(topicsList.toList())
                            etNewTopic.text.clear()
                            
                            Toast.makeText(ctx, R.string.topic_added, Toast.LENGTH_SHORT).show()
                            
                            // Refresh the pie chart
                            if (isAdded && _binding != null) {
                                refreshPieChart()
                            }
                            descDialog.dismiss()
                        }
                    }.onFailure {
                        context?.let { Toast.makeText(it, R.string.topic_add_failed, Toast.LENGTH_SHORT).show() }
                    }
                } catch (e: Exception) {
                    context?.let { Toast.makeText(it, R.string.topic_add_failed, Toast.LENGTH_SHORT).show() }
                }
            }
        }
        
        descDialog.show()
    }
    
    private fun loadTopicsForDialog(topicsList: MutableList<ManageTopicItem>, adapter: ManageTopicsAdapter) {
        // Load topics from Firebase
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded) return@launch
                
                val result = topicRepository.getAllTopics()
                result.onSuccess { topics ->
                    topicsList.clear()
                    context?.let { ctx ->
                        topics.forEachIndexed { index, topic ->
                            val colorIndex = index % topicColors.size
                            topicsList.add(
                                ManageTopicItem(
                                    id = topic.id,
                                    name = topic.name,
                                    description = topic.description,
                                    color = ContextCompat.getColor(ctx, topicColors[colorIndex])
                                )
                            )
                        }
                        adapter.submitList(topicsList.toList())
                    }
                }.onFailure {
                    // Show error or use cached data
                    if (isAdded) {
                        loadCachedTopicsForDialog(topicsList, adapter)
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    loadCachedTopicsForDialog(topicsList, adapter)
                }
            }
        }
    }
    
    private fun loadCachedTopicsForDialog(topicsList: MutableList<ManageTopicItem>, adapter: ManageTopicsAdapter) {
        // Use cached Firebase topics if available
        if (cachedFirebaseTopics.isNotEmpty()) {
            topicsList.clear()
            context?.let { ctx ->
                cachedFirebaseTopics.forEachIndexed { index, topic ->
                    val colorIndex = index % topicColors.size
                    topicsList.add(
                        ManageTopicItem(
                            id = topic.id,
                            name = topic.name,
                            description = topic.description,
                            color = ContextCompat.getColor(ctx, topicColors[colorIndex])
                        )
                    )
                }
                adapter.submitList(topicsList.toList())
            }
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
                context?.let { Toast.makeText(it, R.string.topic_name_empty, Toast.LENGTH_SHORT).show() }
                return@setOnClickListener
            }
            
            if (newDescription.isEmpty()) {
                context?.let { Toast.makeText(it, R.string.topic_description_empty, Toast.LENGTH_SHORT).show() }
                return@setOnClickListener
            }
            
            // Check if name already exists (excluding current topic)
            if (topicsList.any { it.id != topic.id && it.name.equals(newName, ignoreCase = true) }) {
                context?.let { Toast.makeText(it, R.string.topic_already_exists, Toast.LENGTH_SHORT).show() }
                return@setOnClickListener
            }
            
            // Update in Firebase
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (!isAdded) return@launch
                    
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
                        
                        context?.let { Toast.makeText(it, R.string.topic_updated, Toast.LENGTH_SHORT).show() }
                        
                        // Refresh the pie chart
                        if (isAdded && _binding != null) {
                            refreshPieChart()
                        }
                        editDialog.dismiss()
                    }.onFailure {
                        context?.let { Toast.makeText(it, R.string.topic_update_failed, Toast.LENGTH_SHORT).show() }
                    }
                } catch (e: Exception) {
                    context?.let { Toast.makeText(it, R.string.topic_update_failed, Toast.LENGTH_SHORT).show() }
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
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (!isAdded) return@launch
                        
                        val result = topicRepository.deleteTopic(topic.id)
                        result.onSuccess {
                            topicsList.remove(topic)
                            (parentDialog.findViewById<RecyclerView>(R.id.rvTopics)?.adapter as? ManageTopicsAdapter)
                                ?.submitList(topicsList.toList())
                            
                            context?.let { Toast.makeText(it, R.string.topic_deleted, Toast.LENGTH_SHORT).show() }
                            
                            // Refresh the pie chart
                            if (isAdded && _binding != null) {
                                refreshPieChart()
                            }
                        }.onFailure {
                            context?.let { Toast.makeText(it, R.string.topic_delete_failed, Toast.LENGTH_SHORT).show() }
                        }
                    } catch (e: Exception) {
                        context?.let { Toast.makeText(it, R.string.topic_delete_failed, Toast.LENGTH_SHORT).show() }
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
