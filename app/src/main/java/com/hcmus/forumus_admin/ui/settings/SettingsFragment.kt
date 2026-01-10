package com.hcmus.forumus_admin.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hcmus.forumus_admin.R

class SettingsFragment : Fragment() {

    private var selectedTheme: Theme = Theme.LIGHT
    private var selectedLanguage: Language = Language.ENGLISH

    // Theme buttons and their components
    private lateinit var lightThemeButton: FrameLayout
    private lateinit var lightThemeIconContainer: FrameLayout
    private lateinit var lightThemeIcon: ImageView
    private lateinit var lightThemeText: TextView
    private lateinit var lightThemeIndicator: FrameLayout

    private lateinit var lightThemeIndicatorDot: View

    private lateinit var menuIcon: FrameLayout

    private lateinit var darkThemeButton: FrameLayout
    private lateinit var darkThemeIconContainer: FrameLayout
    private lateinit var darkThemeIcon: ImageView
    private lateinit var darkThemeText: TextView
    private lateinit var darkThemeIndicator: FrameLayout
    private lateinit var darkThemeIndicatorDot: View

    private lateinit var autoThemeButton: FrameLayout
    private lateinit var autoThemeIconContainer: FrameLayout
    private lateinit var autoThemeIcon: ImageView
    private lateinit var autoThemeText: TextView
    private lateinit var autoThemeIndicator: FrameLayout
    private lateinit var autoThemeIndicatorDot: View

    // Language buttons and their components
    private lateinit var englishLanguageButton: FrameLayout
    private lateinit var englishLanguageIndicator: FrameLayout

    private lateinit var vietnameseLanguageButton: FrameLayout
    private lateinit var vietnameseLanguageIndicator: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        
        // Load saved preferences
        loadPreferences()
        
        // Set initial selection
        updateThemeSelection(selectedTheme)
        updateLanguageSelection(selectedLanguage)
    }

    private fun initializeViews(view: View) {
        menuIcon = view.findViewById(R.id.menuIcon)

        // Theme buttons
        lightThemeButton = view.findViewById(R.id.lightThemeButton)
        lightThemeIconContainer = view.findViewById(R.id.lightThemeIconContainer)
        lightThemeIcon = view.findViewById(R.id.lightThemeIcon)
        lightThemeText = view.findViewById(R.id.lightThemeText)
        lightThemeIndicator = view.findViewById(R.id.lightThemeIndicator)
        lightThemeIndicatorDot = view.findViewById(R.id.lightThemeIndicatorDot)

        darkThemeButton = view.findViewById(R.id.darkThemeButton)
        darkThemeIconContainer = view.findViewById(R.id.darkThemeIconContainer)
        darkThemeIcon = view.findViewById(R.id.darkThemeIcon)
        darkThemeText = view.findViewById(R.id.darkThemeText)
        darkThemeIndicator = view.findViewById(R.id.darkThemeIndicator)
        darkThemeIndicatorDot = view.findViewById(R.id.darkThemeIndicatorDot)

        autoThemeButton = view.findViewById(R.id.autoThemeButton)
        autoThemeIconContainer = view.findViewById(R.id.autoThemeIconContainer)
        autoThemeIcon = view.findViewById(R.id.autoThemeIcon)
        autoThemeText = view.findViewById(R.id.autoThemeText)
        autoThemeIndicator = view.findViewById(R.id.autoThemeIndicator)
        autoThemeIndicatorDot = view.findViewById(R.id.autoThemeIndicatorDot)

        // Language buttons
        englishLanguageButton = view.findViewById(R.id.englishLanguageButton)
        englishLanguageIndicator = view.findViewById(R.id.englishLanguageIndicator)

        vietnameseLanguageButton = view.findViewById(R.id.vietnameseLanguageButton)
        vietnameseLanguageIndicator = view.findViewById(R.id.vietnameseLanguageIndicator)
    }

    private fun setupClickListeners() {
        menuIcon.setOnClickListener {
            (activity as? com.hcmus.forumus_admin.MainActivity)?.openDrawer()
        }

        lightThemeButton.setOnClickListener {
            selectedTheme = Theme.LIGHT
            updateThemeSelection(Theme.LIGHT)
            savePreferences()
            Toast.makeText(context, "Light theme selected", Toast.LENGTH_SHORT).show()
        }

        darkThemeButton.setOnClickListener {
            selectedTheme = Theme.DARK
            updateThemeSelection(Theme.DARK)
            savePreferences()
            Toast.makeText(context, "Dark theme selected", Toast.LENGTH_SHORT).show()
        }

        autoThemeButton.setOnClickListener {
            selectedTheme = Theme.AUTO
            updateThemeSelection(Theme.AUTO)
            savePreferences()
            Toast.makeText(context, "Auto theme selected", Toast.LENGTH_SHORT).show()
        }

        englishLanguageButton.setOnClickListener {
            selectedLanguage = Language.ENGLISH
            updateLanguageSelection(Language.ENGLISH)
            savePreferences()
            Toast.makeText(context, "English selected", Toast.LENGTH_SHORT).show()
        }

        vietnameseLanguageButton.setOnClickListener {
            selectedLanguage = Language.VIETNAMESE
            updateLanguageSelection(Language.VIETNAMESE)
            savePreferences()
            Toast.makeText(context, "Tiếng Việt được chọn", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateThemeSelection(theme: Theme) {
        // Reset all theme buttons to default state
        resetThemeButton(lightThemeButton, lightThemeIconContainer, lightThemeIcon, lightThemeText, lightThemeIndicator)
        resetThemeButton(darkThemeButton, darkThemeIconContainer, darkThemeIcon, darkThemeText, darkThemeIndicator)
        resetThemeButton(autoThemeButton, autoThemeIconContainer, autoThemeIcon, autoThemeText, autoThemeIndicator)

        // Apply selected state to the chosen theme
        when (theme) {
            Theme.LIGHT -> {
                applySelectedTheme(
                    lightThemeButton,
                    lightThemeIconContainer,
                    lightThemeIcon,
                    lightThemeText,
                    lightThemeIndicator,
                    lightThemeIndicatorDot,
                    R.drawable.bg_theme_button_light_selected,
                    R.color.settings_button_icon_bg_selected_light,
                    R.color.settings_indicator_dot_orange
                )
            }
            Theme.DARK -> {
                applySelectedTheme(
                    darkThemeButton,
                    darkThemeIconContainer,
                    darkThemeIcon,
                    darkThemeText,
                    darkThemeIndicator,
                    darkThemeIndicatorDot,
                    R.drawable.bg_theme_button_dark_selected,
                    R.color.settings_button_icon_bg_selected_dark,
                    R.color.settings_indicator_dot_dark
                )
            }
            Theme.AUTO -> {
                applySelectedTheme(
                    autoThemeButton,
                    autoThemeIconContainer,
                    autoThemeIcon,
                    autoThemeText,
                    autoThemeIndicator,
                    autoThemeIndicatorDot,
                    R.drawable.bg_theme_button_auto_selected,
                    R.color.settings_button_icon_bg_semi,
                    R.color.settings_blue
                )
            }
        }
    }

    private fun resetThemeButton(
        button: FrameLayout,
        iconContainer: FrameLayout,
        icon: ImageView,
        text: TextView,
        indicator: FrameLayout
    ) {
        button.setBackgroundResource(R.drawable.bg_theme_button)
        iconContainer.setBackgroundResource(R.drawable.bg_theme_icon)
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.settings_text_primary))
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.settings_text_primary))
        indicator.visibility = View.GONE
    }

    private fun applySelectedTheme(
        button: FrameLayout,
        iconContainer: FrameLayout,
        icon: ImageView,
        text: TextView,
        indicator: FrameLayout,
        indicatorDot: View,
        backgroundRes: Int,
        iconBgColor: Int,
        indicatorDotColor: Int
    ) {
        button.setBackgroundResource(backgroundRes)
        iconContainer.setBackgroundResource(R.drawable.bg_theme_icon_selected)
        icon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white))
        text.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        indicator.visibility = View.VISIBLE
        indicatorDot.setBackgroundResource(R.drawable.bg_section_indicator)
        indicatorDot.backgroundTintList = ContextCompat.getColorStateList(requireContext(), indicatorDotColor)
    }

    private fun updateLanguageSelection(language: Language) {
        // Reset both language buttons to default state
        englishLanguageButton.setBackgroundResource(R.drawable.bg_language_button)
        englishLanguageIndicator.visibility = View.GONE

        vietnameseLanguageButton.setBackgroundResource(R.drawable.bg_language_button)
        vietnameseLanguageIndicator.visibility = View.GONE

        // Apply selected state to the chosen language
        when (language) {
            Language.ENGLISH -> {
                englishLanguageButton.setBackgroundResource(R.drawable.bg_language_button_selected)
                englishLanguageIndicator.visibility = View.VISIBLE
            }
            Language.VIETNAMESE -> {
                vietnameseLanguageButton.setBackgroundResource(R.drawable.bg_language_button_selected)
                vietnameseLanguageIndicator.visibility = View.VISIBLE
            }
        }
    }

    private fun loadPreferences() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val themeName = prefs.getString("theme", "LIGHT") ?: "LIGHT"
        val languageName = prefs.getString("language", "ENGLISH") ?: "ENGLISH"

        selectedTheme = Theme.valueOf(themeName)
        selectedLanguage = Language.valueOf(languageName)
    }

    private fun savePreferences() {
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("theme", selectedTheme.name)
            putString("language", selectedLanguage.name)
            apply()
        }
    }

    enum class Theme {
        LIGHT, DARK, AUTO
    }

    enum class Language {
        ENGLISH, VIETNAMESE
    }
}
