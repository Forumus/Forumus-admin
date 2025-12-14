package com.hcmus.forumus_admin.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
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
 * Shows both user names and IDs in the dropdown
 */
class UserAutoCompleteAdapter(
    context: Context,
    private var suggestions: List<UserSuggestion> = emptyList()
) : ArrayAdapter<UserSuggestion>(context, R.layout.item_autocomplete_suggestion, suggestions), Filterable {

    private var filteredSuggestions: List<UserSuggestion> = suggestions

    override fun getCount(): Int = filteredSuggestions.size

    override fun getItem(position: Int): UserSuggestion? = 
        if (position in filteredSuggestions.indices) filteredSuggestions[position] else null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_autocomplete_suggestion, parent, false)
        
        val suggestion = getItem(position)
        val textView = view.findViewById<TextView>(R.id.suggestionText) ?: view as? TextView
        textView?.text = suggestion?.displayText ?: ""
        
        return view
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
            val query = constraint?.toString()?.lowercase()?.trim() ?: ""
            
            val filtered = if (query.isEmpty()) {
                suggestions
            } else {
                suggestions.filter { suggestion ->
                    suggestion.name.lowercase().contains(query) ||
                    suggestion.id.lowercase().contains(query)
                }
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
            return (resultValue as? UserSuggestion)?.displayText ?: ""
        }
    }
}
