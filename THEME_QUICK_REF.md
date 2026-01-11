# Theme Switching - Quick Reference

## How to Use (As a User)

1. Open the app and navigate to **Settings** (via navigation drawer)
2. In the **Appearance** section, you'll see three theme buttons:
   - **Light** ☀️ - Always uses light theme
   - **Dark** 🌙 - Always uses dark theme
   - **Auto** 🔄 - Follows your device's system theme
3. Tap any button to switch themes **immediately**
4. Your choice is **automatically saved** and will persist even after closing the app

## How It Works (Technical)

### ThemeManager API

```kotlin
// Apply saved theme
ThemeManager.applyTheme(context)

// Apply specific theme
ThemeManager.applyTheme(ThemeManager.ThemeMode.LIGHT)
ThemeManager.applyTheme(ThemeManager.ThemeMode.DARK)
ThemeManager.applyTheme(ThemeManager.ThemeMode.AUTO)

// Save theme preference
ThemeManager.saveThemeMode(context, ThemeManager.ThemeMode.DARK)

// Get saved theme
val currentTheme = ThemeManager.getSavedThemeMode(context)
```

### Theme Modes

- **LIGHT**: `AppCompatDelegate.MODE_NIGHT_NO`
- **DARK**: `AppCompatDelegate.MODE_NIGHT_YES`
- **AUTO**: `AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM`

### Where Themes Are Applied

1. **Application Level**: `ForumusAdminApplication.onCreate()`
   - Applies saved theme before any activity starts
   
2. **Activity Level**: `MainActivity.onCreate()`
   - Re-applies theme for safety
   - Configures status bar colors
   
3. **Settings Screen**: `SettingsFragment`
   - User interaction point
   - Immediate theme switching

### Color Resources

- **Light Mode**: `res/values/colors.xml`
- **Dark Mode**: `res/values-night/colors.xml`

Key colors that change:
- `background_light`: #F8FAFC (light) → #121212 (dark)
- `text_primary`: #1E293B (light) → #E2E8F0 (dark)
- `card_background`: #FFFFFF (light) → #1E293B (dark)
- `border_gray`: #e0e0e0 (light) → #2D3748 (dark)

### Theme Resources

- **Light Theme**: `res/values/themes.xml`
- **Dark Theme**: `res/values-night/themes.xml`

Both use `Theme.Material3.DayNight.NoActionBar` as parent for automatic switching.

## Common Issues & Solutions

### Theme doesn't persist after app restart
- Check ThemeManager is called in ForumusAdminApplication.onCreate()
- Verify SharedPreferences are being saved correctly

### Status bar colors don't update
- Ensure MainActivity.setupStatusBar() is being called
- Check that Configuration.UI_MODE_NIGHT_MASK is properly detected

### Some UI elements don't change theme
- Verify they use theme colors from colors.xml (not hardcoded)
- Check if they reference the correct color resource names

### Auto mode doesn't follow system
- Make sure MODE_NIGHT_FOLLOW_SYSTEM is being set
- Test on Android 10+ where this feature works best

## File Locations

```
app/src/main/
├── java/com/hcmus/forumus_admin/
│   ├── ForumusAdminApplication.kt (applies theme on app start)
│   ├── MainActivity.kt (configures status bar)
│   ├── core/
│   │   └── ThemeManager.kt (core theme logic)
│   └── ui/settings/
│       └── SettingsFragment.kt (user interface)
└── res/
    ├── values/
    │   ├── colors.xml (light mode colors)
    │   └── themes.xml (light theme config)
    └── values-night/
        ├── colors.xml (dark mode colors)
        └── themes.xml (dark theme config)
```

## Testing Checklist

- [ ] Light mode button works and applies immediately
- [ ] Dark mode button works and applies immediately
- [ ] Auto mode button works and follows system
- [ ] Theme persists after app restart
- [ ] Status bar colors update correctly
- [ ] All screens adapt to theme (Dashboard, Settings, Blacklist, etc.)
- [ ] Charts and graphs are visible in both themes
- [ ] Text has proper contrast in both themes
- [ ] Navigation drawer adapts to theme

## Migration Notes

If you had custom theme code before:
- Remove any local theme enums (use ThemeManager.ThemeMode)
- Remove manual SharedPreferences for theme (use ThemeManager methods)
- Remove any hardcoded theme application (use ThemeManager.applyTheme())
