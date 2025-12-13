package com.hcmus.forumus_admin.ui.assistant

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_admin.MainActivity
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.databinding.FragmentAiModerationBinding

class AssistantFragment : Fragment() {

    private var _binding: FragmentAiModerationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AiModerationViewModel by viewModels()
    private lateinit var adapter: AiPostsAdapter

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
        
        // TODO: Add more options menu functionality
        binding.moreOptionsIcon.setOnClickListener {
            // Show options menu
        }
        
        // TODO: Add sort functionality
        binding.sortButton.setOnClickListener {
            // Show sort options
        }
        
        // TODO: Add filter functionality
        binding.filterButton.setOnClickListener {
            // Show filter options
        }
    }
    
    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            updateTabSelection(state.currentTab)
            adapter.submitList(state.filteredPosts)
            
            // Show/hide loading indicator
//            _binding?.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            
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
