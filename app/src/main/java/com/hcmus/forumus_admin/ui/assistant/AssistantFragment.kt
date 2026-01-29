package com.hcmus.forumus_admin.ui.assistant

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_admin.MainActivity
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.Violation
import com.hcmus.forumus_admin.data.repository.ViolationRepository
import com.hcmus.forumus_admin.databinding.FragmentAiModerationBinding
import kotlinx.coroutines.launch

class AssistantFragment : Fragment() {

    private var _binding: FragmentAiModerationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AiModerationViewModel by viewModels()
    private lateinit var adapter: AiPostsAdapter
    private val violationRepository = ViolationRepository()
    
    // Current filter state for dialogs
    private var currentSortOrder = SortOrder.NEWEST_FIRST
    private val selectedViolationIds = mutableSetOf<String>()  // Changed to store violation IDs
    private var availableViolations: List<Violation> = emptyList()  // Store all available violations

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiModerationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupTabs()
        setupSearchField()
        setupToolbar()
        observeViewModel()
        loadViolationTypes()
    }
    
    private fun loadViolationTypes() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = violationRepository.getAllViolations()
                result.onSuccess { violations ->
                    availableViolations = violations
                }.onFailure { exception ->
                    context?.let {
                        Toast.makeText(it, getString(R.string.load_violation_types_failed, exception.message), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                context?.let {
                    Toast.makeText(it, getString(R.string.error_loading_violations, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = AiPostsAdapter(
            onApprove = { postId ->
                val post = viewModel.state.value?.allPosts?.find { it.postData.id == postId }
                if (post != null) {
                    showApproveConfirmation(post)
                }
            },
            onReject = { postId ->
                val post = viewModel.state.value?.allPosts?.find { it.postData.id == postId }
                if (post != null) {
                    showRejectConfirmation(post)
                }
            }
        )
        
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AssistantFragment.adapter
        }
    }

    private fun showApproveConfirmation(post: com.hcmus.forumus_admin.data.model.AiModerationResult) {
        val message = if (viewModel.state.value?.currentTab == TabType.AI_REJECTED) {
            getString(R.string.confirm_approve_rejected_post)
        } else {
             getString(R.string.confirm_approve_post)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.approve_post_title))
            .setMessage("$message\n\nTitle: ${post.postData.title}")
            .setPositiveButton(getString(R.string.approve)) { _, _ ->
                viewModel.approvePost(post.postData.id)
                Toast.makeText(requireContext(), getString(R.string.post_approved_toast), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showRejectConfirmation(post: com.hcmus.forumus_admin.data.model.AiModerationResult) {
        val isApprovedTab = viewModel.state.value?.currentTab == TabType.AI_APPROVED
        val title = if (isApprovedTab) getString(R.string.reject_post_title) else getString(R.string.delete_post_text)
        val message = getString(R.string.confirm_reject_delete_message)

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("$message\n\nTitle: ${post.postData.title}")
            .setPositiveButton(getString(R.string.reject)) { _, _ ->
                // Only send notification if rejecting from AI Approved tab
                viewModel.rejectPost(post.postData.id, sendNotification = isApprovedTab)
                Toast.makeText(requireContext(), getString(R.string.post_rejected_toast), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun setupTabs() {
        binding.tabApproved.setOnClickListener {
            viewModel.selectTab(TabType.AI_APPROVED)
        }
        
        binding.tabRejected.setOnClickListener {
            viewModel.selectTab(TabType.AI_REJECTED)
        }
    }
    
    private fun setupSearchField() {
        binding.searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchPosts(s?.toString() ?: "")
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupToolbar() {
        binding.menuIcon.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
        

        
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }
    
    private fun showSortDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sort_time, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent for rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Track selection
        var selectedOrder = currentSortOrder
        
        // Get views
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val newestFirstButton = dialogView.findViewById<View>(R.id.newestFirstButton)
        val oldestFirstButton = dialogView.findViewById<View>(R.id.oldestFirstButton)
        val newestFirstIndicator = dialogView.findViewById<View>(R.id.newestFirstIndicator)
        val oldestFirstIndicator = dialogView.findViewById<View>(R.id.oldestFirstIndicator)
        val applyButton = dialogView.findViewById<View>(R.id.applyButton)
        
        // Initialize UI based on current selection
        updateSortButtonState(newestFirstButton, newestFirstIndicator, selectedOrder == SortOrder.NEWEST_FIRST)
        updateSortButtonState(oldestFirstButton, oldestFirstIndicator, selectedOrder == SortOrder.OLDEST_FIRST)
        
        closeButton.setOnClickListener { dialog.dismiss() }
        
        newestFirstButton.setOnClickListener {
            selectedOrder = SortOrder.NEWEST_FIRST
            updateSortButtonState(newestFirstButton, newestFirstIndicator, true)
            updateSortButtonState(oldestFirstButton, oldestFirstIndicator, false)
        }
        
        oldestFirstButton.setOnClickListener {
            selectedOrder = SortOrder.OLDEST_FIRST
            updateSortButtonState(newestFirstButton, newestFirstIndicator, false)
            updateSortButtonState(oldestFirstButton, oldestFirstIndicator, true)
        }
        
        applyButton.setOnClickListener {
            currentSortOrder = selectedOrder
            viewModel.setSortOrder(selectedOrder)
            
            val message = if (selectedOrder == SortOrder.NEWEST_FIRST) {
                getString(R.string.sorted_by_newest)
            } else {
                getString(R.string.sorted_by_oldest)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateSortButtonState(button: View, indicator: View, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundResource(R.drawable.bg_sort_order_selected)
            indicator.setBackgroundResource(R.drawable.bg_radio_order_selected)
        } else {
            button.setBackgroundResource(R.drawable.bg_sort_order_unselected)
            indicator.setBackgroundResource(R.drawable.bg_radio_order_unselected)
        }
    }
    
    private fun showFilterDialog() {
        if (availableViolations.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.loading_violation_types), Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_violation, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent for rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Temp selection (copy of current selection)
        val tempSelectedIds = selectedViolationIds.toMutableSet()
        
        // Get views
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val clearAllButton = dialogView.findViewById<View>(R.id.clearAllButton)
        val applyButton = dialogView.findViewById<View>(R.id.applyButton)
        val optionsContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.violationOptionsContainer)
        
        if (optionsContainer != null) {
            // Clear existing static options
            optionsContainer.removeAllViews()
            
            // Create dynamic violation buttons from Firebase data
            val violationButtons = mutableMapOf<String, Pair<LinearLayout, ImageView>>()
            
            availableViolations.forEach { violation ->
                val buttonLayout = LayoutInflater.from(requireContext()).inflate(
                    R.layout.item_violation_filter_option, 
                    optionsContainer, 
                    false
                ) as? LinearLayout
                
                if (buttonLayout != null) {
                    val textView = buttonLayout.findViewById<android.widget.TextView>(R.id.violationText)
                    val checkmark = buttonLayout.findViewById<ImageView>(R.id.violationCheckmark)
                    
                    textView?.text = violation.name
                    
                    // Initialize selection state
                    updateFilterButtonState(buttonLayout, checkmark, violation.violation in tempSelectedIds)
                    
                    // Add click listener
                    buttonLayout.setOnClickListener {
                        val isSelected = violation.violation in tempSelectedIds
                        if (isSelected) {
                            tempSelectedIds.remove(violation.violation)
                        } else {
                            tempSelectedIds.add(violation.violation)
                        }
                        updateFilterButtonState(buttonLayout, checkmark, !isSelected)
                    }
                    
                    optionsContainer.addView(buttonLayout)
                    violationButtons[violation.violation] = Pair(buttonLayout, checkmark)
                }
            }
            
            closeButton.setOnClickListener { dialog.dismiss() }
            
            clearAllButton.setOnClickListener {
                tempSelectedIds.clear()
                violationButtons.forEach { (_, views) ->
                    updateFilterButtonState(views.first, views.second, false)
                }
            }
            
            applyButton.setOnClickListener {
                selectedViolationIds.clear()
                selectedViolationIds.addAll(tempSelectedIds)
                viewModel.setViolationFilter(selectedViolationIds)
                
                val message = if (tempSelectedIds.isEmpty()) {
                    getString(R.string.filter_cleared)
                } else {
                    val selectedNames = availableViolations
                        .filter { it.violation in tempSelectedIds }
                        .joinToString(", ") { it.name }
                    getString(R.string.filtered_by, selectedNames)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        } else {
            // Fallback: Use existing static buttons if container not found
            setupStaticFilterButtons(dialogView, tempSelectedIds, dialog)
        }
        
        dialog.show()
    }
    
    private fun setupStaticFilterButtons(
        dialogView: View,
        tempSelectedIds: MutableSet<String>,
        dialog: AlertDialog
    ) {
        // Fallback implementation using static buttons
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val clearAllButton = dialogView.findViewById<View>(R.id.clearAllButton)
        val applyButton = dialogView.findViewById<View>(R.id.applyButton)
        
        // Map static button IDs to violation IDs (based on common violation codes)
        val buttonMapping = mapOf(
            R.id.toxicityButton to "vio_001",
            R.id.severeToxicityButton to "vio_002",
            R.id.identityAttackButton to "vio_003",
            R.id.insultButton to "vio_004",
            R.id.profanityButton to "vio_005",
            R.id.threatButton to "vio_006",
            R.id.spamButton to "vio_007",
            R.id.sexuallyExplicitButton to "vio_008",
            R.id.misinformationButton to "vio_009"
        )
        
        val violationButtons = mutableMapOf<String, Pair<LinearLayout, ImageView>>()
        buttonMapping.forEach { (buttonId, violationId) ->
            val button = dialogView.findViewById<LinearLayout>(buttonId)
            val checkmarkId = when(buttonId) {
                R.id.toxicityButton -> R.id.toxicityCheckmark
                R.id.severeToxicityButton -> R.id.severeToxicityCheckmark
                R.id.identityAttackButton -> R.id.identityAttackCheckmark
                R.id.insultButton -> R.id.insultCheckmark
                R.id.profanityButton -> R.id.profanityCheckmark
                R.id.threatButton -> R.id.threatCheckmark
                R.id.spamButton -> R.id.spamCheckmark
                R.id.sexuallyExplicitButton -> R.id.sexuallyExplicitCheckmark
                R.id.misinformationButton -> R.id.misinformationCheckmark
                else -> 0
            }
            val checkmark = dialogView.findViewById<ImageView>(checkmarkId)
            
            if (button != null && checkmark != null) {
                updateFilterButtonState(button, checkmark, violationId in tempSelectedIds)
                button.setOnClickListener {
                    val isSelected = violationId in tempSelectedIds
                    if (isSelected) {
                        tempSelectedIds.remove(violationId)
                    } else {
                        tempSelectedIds.add(violationId)
                    }
                    updateFilterButtonState(button, checkmark, !isSelected)
                }
                violationButtons[violationId] = Pair(button, checkmark)
            }
        }
        
        closeButton.setOnClickListener { dialog.dismiss() }
        
        clearAllButton.setOnClickListener {
            tempSelectedIds.clear()
            violationButtons.forEach { (_, views) ->
                updateFilterButtonState(views.first, views.second, false)
            }
        }
        
        applyButton.setOnClickListener {
            selectedViolationIds.clear()
            selectedViolationIds.addAll(tempSelectedIds)
            viewModel.setViolationFilter(selectedViolationIds)
            
            val message = if (tempSelectedIds.isEmpty()) {
                getString(R.string.filter_cleared)
            } else {
                val selectedNames = availableViolations
                    .filter { it.violation in tempSelectedIds }
                    .joinToString(", ") { it.name }
                getString(R.string.filtered_by, selectedNames)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }
    
    private fun updateFilterButtonState(button: View, checkmark: ImageView, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundResource(R.drawable.bg_badge_remind)
            checkmark.visibility = View.VISIBLE
        } else {
            button.setBackgroundResource(R.drawable.bg_filter_option_unselected)
            checkmark.visibility = View.GONE
        }
    }
    
    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            updateTabSelection(state.currentTab)
            adapter.submitList(state.filteredPosts)
            
            // Update loading states for individual posts
            adapter.setLoadingPostIds(state.loadingPostIds)
            
            // Update sort/filter state from ViewModel
            currentSortOrder = state.sortOrder
            selectedViolationIds.clear()
            selectedViolationIds.addAll(state.selectedViolationIds)
            
            // Show/hide loading indicator (for full list loading)
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            
            // Show/hide empty state
            if (state.filteredPosts.isEmpty() && !state.isLoading) {
                binding.emptyState.visibility = View.VISIBLE
                binding.postsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.postsRecyclerView.visibility = View.VISIBLE
            }
        }
    }
    
    private fun updateTabSelection(selectedTab: TabType) {
        when (selectedTab) {
            TabType.AI_APPROVED -> {
                // Update approved tab
                binding.tabApprovedText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.tab_selected)
                )
                binding.tabApprovedIndicator.visibility = View.VISIBLE
                
                // Update rejected tab
                binding.tabRejectedText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.tab_unselected)
                )
                binding.tabRejectedIndicator.visibility = View.INVISIBLE
            }
            TabType.AI_REJECTED -> {
                // Update approved tab
                binding.tabApprovedText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.tab_unselected)
                )
                binding.tabApprovedIndicator.visibility = View.INVISIBLE
                
                // Update rejected tab
                binding.tabRejectedText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.tab_selected)
                )
                binding.tabRejectedIndicator.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
