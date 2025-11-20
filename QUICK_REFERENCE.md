# Admin Dashboard - Quick Reference Guide

## Screen Layout Structure

```
┌─────────────────────────────────────┐
│         [HEADER - 70dp]             │
│   Forumus Logo + User Avatar        │
├─────────────────────────────────────┤
│                                     │
│  ┌──────────┐  ┌──────────┐       │
│  │ [👥]     │  │ [📄]     │       │
│  │ Total    │  │ Total    │       │
│  │ Users    │  │ Posts    │       │
│  │ 1,234    │  │ 5,678    │       │
│  └──────────┘  └──────────┘       │
│                                     │
│  ┌──────────┐  ┌──────────┐       │
│  │ [🚫]     │  │ [⚠️]     │       │
│  │ Black-   │  │ Reported │       │
│  │ listed   │  │ Posts    │       │
│  │ 23       │  │ 45       │       │
│  └──────────┘  └──────────┘       │
│                                     │
│  ┌─────────────────────────────┐  │
│  │ Posts Over Time             │  │
│  │         [Day|Week|Month]    │  │
│  │                             │  │
│  │      ┌──┐                   │  │
│  │      │  │    ┌──┐           │  │
│  │ ┌──┐ │  │ ┌──┤  │           │  │
│  │ │  │ │  │ │  │  │ ┌──┐     │  │
│  │ └──┘ └──┘ └──┘ └──┘ └──┘   │  │
│  │  W1   W2   W3   W4          │  │
│  └─────────────────────────────┘  │
│                                     │
│  ┌─────────────────────────────┐  │
│  │ Posts by Topic   [Manage]   │  │
│  │                             │  │
│  │        ╱─────╲              │  │
│  │       │       │             │  │
│  │        ╲─────╱              │  │
│  │                             │  │
│  │  ● Education                │  │
│  │  ● Entertainment            │  │
│  │  ● Others                   │  │
│  │  ● Sports                   │  │
│  │  ● Technology               │  │
│  └─────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

## Component Breakdown

### 1. Stat Cards (4 cards in 2x2 grid)
Each card contains:
- **Icon Container**: 48dp x 48dp, colored background, rounded corners
- **Label**: 12sp, gray color (#666666)
- **Value**: 24sp, bold, dark color (#2c2c2c)

**Colors:**
- Users: Blue (#3f78e0)
- Posts: Green (#19d25a)
- Blacklisted: Red (#e7000b)
- Reported: Orange (#f59019)

### 2. Bar Chart Card
- **Height**: 200dp
- **Title**: "Posts Over Time" (16sp, bold)
- **Toggle Buttons**: Day/Week/Month
- **Chart Type**: Vertical Bar Chart
- **Data**: 4 bars representing weeks
- **Colors**: Blue bars (#3f78e0)

### 3. Pie Chart Card
- **Height**: 250dp
- **Title**: "Posts by Topic" (16sp, bold)
- **Manage Button**: Outlined button with settings icon
- **Chart Type**: Pie Chart (no hole in center)
- **Data**: 5 topic categories
- **Legend**: Bottom-left, vertical orientation

## Key Features Implemented

✅ Responsive layout with ScrollView
✅ Material Design 3 components
✅ View Binding for type-safe view access
✅ MPAndroidChart for interactive charts
✅ Custom colors matching Figma design
✅ Reusable stat card component
✅ Toggle button group for time ranges
✅ Animated chart rendering
✅ Proper spacing and elevation

## Color Palette

| Purpose           | Color Code | Usage                    |
|-------------------|------------|--------------------------|
| Primary Blue      | #3f78e0    | Users icon, Technology   |
| Success Green     | #19d25a    | Posts icon               |
| Danger Red        | #e7000b    | Blacklist, Entertainment |
| Warning Orange    | #f59019    | Report icon              |
| Chart Blue        | #1976d2    | Education topic          |
| Chart Brown       | #c49a68    | Sports topic             |
| Text Primary      | #2c2c2c    | Main text                |
| Text Secondary    | #666666    | Labels, legends          |
| Border Gray       | #e0e0e0    | Card borders             |
| Background        | #FFFFFF    | Cards, screen            |

## File Structure

```
app/src/main/
├── java/com/anhkhoa/forumus_admin/
│   └── ui/dashboard/
│       └── DashboardFragment.kt
├── res/
│   ├── drawable/
│   │   ├── ic_users.xml
│   │   ├── ic_posts.xml
│   │   ├── ic_block.xml
│   │   ├── ic_report.xml
│   │   ├── ic_settings.xml
│   │   └── bg_stat_icon.xml
│   ├── layout/
│   │   ├── fragment_dashboard.xml
│   │   └── item_stat_card.xml
│   └── values/
│       ├── colors.xml
│       ├── dimens.xml
│       └── strings.xml
└── build.gradle.kts (modified)
```

## Chart Configuration

### Bar Chart Settings
```kotlin
- Bar Width: 0.4f
- Animation: Y-axis, 1000ms
- X-Axis: Bottom position, Week labels
- Y-Axis: Left only, 0-400 range, 100 granularity
- Grid: Horizontal lines only
- Legend: Disabled
```

### Pie Chart Settings
```kotlin
- Hole Radius: 0f (no center hole)
- Slice Space: 2dp
- Animation: Y-axis, 1000ms
- Value Format: Percentage (%)
- Legend: Bottom-left, vertical
- Colors: 5 distinct colors for topics
```

## Next Steps for Integration

1. **Replace sample data** with API calls
2. **Add ViewModel** for data management
3. **Implement navigation** from Manage button
4. **Add error handling** for network failures
5. **Implement refresh** functionality
6. **Add loading indicators** during data fetch
7. **Support dark mode** theme
8. **Add accessibility** labels

## Testing Commands

```bash
# Sync Gradle dependencies
./gradlew clean build

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
```

---
**Implementation Date**: 2025
**Design Source**: Figma - Forumus Screens (Node ID: 343-1491)
**Technology Stack**: Kotlin, XML Layouts, MPAndroidChart, Material Design 3
