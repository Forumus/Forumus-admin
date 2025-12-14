package com.hcmus.forumus_admin.ui.common

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import com.hcmus.forumus_admin.R

/**
 * Data class to hold user suggestion info
 */
data class UserSuggestion(
    val id: String,
    val name: String,
    val displayText: String = "$name ($id)"
)

/**
 * Custom adapter for user autocomplete suggestions
 * Shows both user names and IDs in the dropdown with modern Google-style UI
 */
class UserAutoCompleteAdapter(
    context: Context,
    private var suggestions: List<UserSuggestion> = emptyList()
) : ArrayAdapter<UserSuggestion>(context, R.layout.item_autocomplete_suggestion, suggestions), Filterable {

    private var filteredSuggestions: List<UserSuggestion> = suggestions
    private var currentQuery: String = ""

    override fun getCount(): Int = filteredSuggestions.size

    override fun getItem(position: Int): UserSuggestion? = 
        if (position in filteredSuggestions.indices) filteredSuggestions[position] else null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_autocomplete_suggestion, parent, false)
        
        val suggestion = getItem(position) ?: return view
        
        // Get views
        val iconView = view.findViewById<ImageView>(R.id.suggestionIcon)
        val primaryText = view.findViewById<TextView>(R.id.suggestionPrimaryText)
        val secondaryText = view.findViewById<TextView>(R.id.suggestionSecondaryText)
        val arrowView = view.findViewById<ImageView>(R.id.suggestionArrow)
        
        // Set icon
        iconView?.setImageResource(R.drawable.ic_search)
        
        // Set primary text with bold highlighting for matching part
        val highlightedText = highlightMatchingText(suggestion.name, currentQuery)
        primaryText?.text = highlightedText
        
        // Set secondary text (ID) if different from name
        if (suggestion.id.isNotEmpty() && suggestion.id != suggestion.name) {
            secondaryText?.visibility = View.VISIBLE
            secondaryText?.text = "ID: ${suggestion.id}"
        } else {
            secondaryText?.visibility = View.GONE
        }
        
        // Show arrow on hover/focus (always visible for now)
        arrowView?.visibility = View.GONE
        
        return view
    }
    
    /**
     * Highlights the matching text in bold
     */
    private fun highlightMatchingText(text: String, query: String): SpannableString {
        val spannable = SpannableString(text)
        
        if (query.isNotEmpty()) {
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            var startIndex = lowerText.indexOf(lowerQuery)
            
            while (startIndex >= 0) {
                val endIndex = startIndex + query.length
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                startIndex = lowerText.indexOf(lowerQuery, endIndex)
            }
        }
        
        return spannable
    }

    /**
     * Update the suggestions list
     */
    fun updateSuggestions(newSuggestions: List<UserSuggestion>) {
        suggestions = newSuggestions
        filteredSuggestions = newSuggestions
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.trim() ?: ""
            currentQuery = query
            
            val filtered = if (query.isEmpty()) {
                suggestions.take(8) // Limit initial suggestions
            } else {
                suggestions.filter { suggestion ->
                    suggestion.name.lowercase().contains(query.lowercase()) ||
                    suggestion.id.lowercase().contains(query.lowercase())
                }.take(8) // Limit to 8 suggestions for cleaner UI
            }
            
            return FilterResults().apply {
                values = filtered
                count = filtered.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredSuggestions = (results?.values as? List<UserSuggestion>) ?: emptyList()
            if (filteredSuggestions.isNotEmpty()) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
        
        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as? UserSuggestion)?.name ?: ""
        }
    }
}
