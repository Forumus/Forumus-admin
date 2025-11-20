package com.anhkhoa.forumus_admin.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.anhkhoa.forumus_admin.MainActivity
import com.anhkhoa.forumus_admin.R
import com.anhkhoa.forumus_admin.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setupStatCards() {
        // Total Users Card
        setupStatCard(
            binding.statCardUsers.root,
            R.drawable.ic_total_users,
            R.color.primary_blue,
            getString(R.string.total_users),
            "1,234"
        )

        // Total Posts Card
        setupStatCard(
            binding.statCardPosts.root,
            R.drawable.ic_posts,
            R.color.success_green,
            getString(R.string.total_posts),
            "5,678"
        )

        // Blacklisted Users Card
        setupStatCard(
            binding.statCardBlacklisted.root,
            R.drawable.ic_block,
            R.color.danger_red,
            getString(R.string.blacklisted_users),
            "23"
        )

        // Reported Posts Card
        setupStatCard(
            binding.statCardReported.root,
            R.drawable.ic_report,
            R.color.warning_orange,
            getString(R.string.reported_posts),
            "45"
        )
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

        // Set icon and background color
        iconImage.setImageResource(iconRes)
        iconContainer.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
        
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
        val pieChart = binding.pieChart
        
        // Sample data for posts by topic
        val entries = listOf(
            PieEntry(30f, getString(R.string.education)),
            PieEntry(25f, getString(R.string.entertainment)),
            PieEntry(15f, getString(R.string.others)),
            PieEntry(15f, getString(R.string.sports)),
            PieEntry(15f, getString(R.string.technology))
        )

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.chart_blue),
            ContextCompat.getColor(requireContext(), R.color.danger_red),
            ContextCompat.getColor(requireContext(), R.color.text_secondary),
            ContextCompat.getColor(requireContext(), R.color.chart_brown),
            ContextCompat.getColor(requireContext(), R.color.primary_blue)
        )

        val dataSet = PieDataSet(entries, "Topics").apply {
            setColors(colors)
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
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
            
            // Configure legend
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 12f
                form = Legend.LegendForm.CIRCLE
                formSize = 12f
                xEntrySpace = 8f
                yEntrySpace = 4f
            }
            
            invalidate()
        }
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
