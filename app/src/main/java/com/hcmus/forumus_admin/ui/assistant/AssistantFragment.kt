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
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_admin.MainActivity
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.ViolationCategory
import com.hcmus.forumus_admin.databinding.FragmentAiModerationBinding

class AssistantFragment : Fragment() {

    private var _binding: FragmentAiModerationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AiModerationViewModel by viewModels()
    private lateinit var adapter: AiPostsAdapter
    
    // Current filter state for dialogs
    private var currentSortOrder = SortOrder.NEWEST_FIRST
    private val selectedViolationTypes = mutableSetOf<ViolationCategory>()

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
    }
    
    private fun setupRecyclerView() {
        adapter = AiPostsAdapter(
            onApprove = { postId ->
                viewModel.approvePost(postId)
            },
            onReject = { postId ->
                viewModel.rejectPost(postId)
            }
        )
        
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AssistantFragment.adapter
        }
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
        
        binding.moreOptionsIcon.setOnClickListener {
            // Show options menu
        }
        
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
        
        binding.filterButton.setOnClickListener {
            showFilterDialog()
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_violation, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent for rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Temp selection (copy of current selection)
        val tempSelectedTypes = selectedViolationTypes.toMutableSet()
        
        // Get views
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val clearAllButton = dialogView.findViewById<View>(R.id.clearAllButton)
        val applyButton = dialogView.findViewById<View>(R.id.applyButton)
        
        // Define violation type buttons
        val violationButtons = mapOf(
            ViolationCategory.TOXICITY to Pair(
                dialogView.findViewById<LinearLayout>(R.id.toxicityButton),
                dialogView.findViewById<ImageView>(R.id.toxicityCheckmark)
            ),
            ViolationCategory.SEVERE_TOXICITY to Pair(
                dialogView.findViewById<LinearLayout>(R.id.severeToxicityButton),
                dialogView.findViewById<ImageView>(R.id.severeToxicityCheckmark)
            ),
            ViolationCategory.IDENTITY_ATTACK to Pair(
                dialogView.findViewById<LinearLayout>(R.id.identityAttackButton),
                dialogView.findViewById<ImageView>(R.id.identityAttackCheckmark)
            ),
            ViolationCategory.INSULT to Pair(
                dialogView.findViewById<LinearLayout>(R.id.insultButton),
                dialogView.findViewById<ImageView>(R.id.insultCheckmark)
            ),
            ViolationCategory.PROFANITY to Pair(
                dialogView.findViewById<LinearLayout>(R.id.profanityButton),
                dialogView.findViewById<ImageView>(R.id.profanityCheckmark)
            ),
            ViolationCategory.THREAT to Pair(
                dialogView.findViewById<LinearLayout>(R.id.threatButton),
                dialogView.findViewById<ImageView>(R.id.threatCheckmark)
            ),
            ViolationCategory.SPAM to Pair(
                dialogView.findViewById<LinearLayout>(R.id.spamButton),
                dialogView.findViewById<ImageView>(R.id.spamCheckmark)
            ),
            ViolationCategory.SEXUALLY_EXPLICIT to Pair(
                dialogView.findViewById<LinearLayout>(R.id.sexuallyExplicitButton),
                dialogView.findViewById<ImageView>(R.id.sexuallyExplicitCheckmark)
            ),
            ViolationCategory.MISINFORMATION to Pair(
                dialogView.findViewById<LinearLayout>(R.id.misinformationButton),
                dialogView.findViewById<ImageView>(R.id.misinformationCheckmark)
            )
        )
        
        // Initialize UI based on current selections
        violationButtons.forEach { (category, views) ->
            updateFilterButtonState(views.first, views.second, category in tempSelectedTypes)
        }
        
        // Set click listeners for each violation type
        violationButtons.forEach { (category, views) ->
            views.first.setOnClickListener {
                val isSelected = category in tempSelectedTypes
                if (isSelected) {
                    tempSelectedTypes.remove(category)
                } else {
                    tempSelectedTypes.add(category)
                }
                updateFilterButtonState(views.first, views.second, !isSelected)
            }
        }
        
        closeButton.setOnClickListener { dialog.dismiss() }
        
        clearAllButton.setOnClickListener {
            tempSelectedTypes.clear()
            violationButtons.forEach { (_, views) ->
                updateFilterButtonState(views.first, views.second, false)
            }
        }
        
        applyButton.setOnClickListener {
            selectedViolationTypes.clear()
            selectedViolationTypes.addAll(tempSelectedTypes)
            viewModel.setViolationFilter(selectedViolationTypes)
            
            val message = if (tempSelectedTypes.isEmpty()) {
                getString(R.string.filter_cleared)
            } else {
                getString(R.string.filter_applied, tempSelectedTypes.joinToString(", ") { it.displayName })
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
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
            
            // Update sort/filter state from ViewModel
            currentSortOrder = state.sortOrder
            selectedViolationTypes.clear()
            selectedViolationTypes.addAll(state.selectedViolationTypes)
            
            // Show/hide loading indicator
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
