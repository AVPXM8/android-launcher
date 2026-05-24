# Kux Launcher 🚀

Kux Launcher is a modern, high-performance, and feature-rich Android launcher engineered using clean architecture, Kotlin, XML ViewBinding, and MVVM principles. It is heavily inspired by industry-leading launchers like **Pixel Launcher**, **Nova Launcher**, and **Niagara**, delivering butter-smooth 60 FPS transitions and a premium, dark glassmorphic user experience.

---

## 🌟 Core Features (Up to Phase 3)

### 1. Persistent Cellular Workspace & Dock
- **Custom Grid Layout (`CellLayout`)**: Features a pixel-perfect $5 \times 5$ cellular desktop grid and a persistent $5 \times 1$ bottom Dock grid.
- **Dynamic Folders**: Merges desktop shortcuts into glassmorphic folder popup containers seamlessly.
- **Active Drag Remove Zone**: Dynamically reveals a "Drop here to Remove" drag card at the top, deleting shortcuts when dropped.

### 2. Full-Screen Premium App Drawer
- **Swipe-to-Reveal Gesture**: Smoothly slides the drawer open when swiping up from empty space on the home screen, and dismisses it when swiping down.
- **Glassmorphic Aesthetic**: Designed using deep, dark frosted-glass visuals (`#E6161616` with 90% opacity, rounded top corners, and alpha fades).
- **Desktop Drop Bridge**: Long-pressing an app inside the drawer automatically collapses it instantly and starts floating the icon over the workspace, facilitating easy shortcut placements.

### 3. Real-Time Search System
- **Asynchronous Search Indexer**: Precomputes lowercase case-insensitive strings of app labels and package names on `Dispatchers.Default` background context to keep the UI thread completely unblocked.
- **Debounced Inputs**: Leverages a custom Coroutine debounce utility to filter results gracefully without lag during rapid typing.
- **Dynamic Text Highlighting**: Renders matching characters in bold, custom-colored accent typography directly inside app label items.
- **Empty States**: Shows animated/static modern fallback layouts when no apps match the query.

### 4. Fast Alphabetical Sidebar Indexer
- **Vertical Touch Scrollbar**: Displays a sleek vertical letter strip (`A-Z` and `#`) on the right side of the drawer.
- **Instant Section Scroll**: Touching or sliding along the letters instantly scrolls the grid to the first matching application entry.

### 5. Native Spring Gestures & Depth Transitions
- **Elastic physics**: Employs the native Android `DynamicAnimation` spring system (`SpringForce` medium stiffness and zero bounce) for organic gesture slides.
- **Workspace Layer Depth**: Scales down the underlying desktop workspace layout slightly (from `1.0` to `0.95`) and fades it down as the drawer slides up, providing a premium visual depth effect.

### 6. Multi-tier Local Memory Caching
- **`IconCache`**: A thread-safe, memory-constrained `LruCache` storing decoded application drawables, avoiding repetitive decoding during list scrolls.
- **`AppInfoCache`**: Caches package lists fetched from the system `PackageManager` to bypass expensive OS queries during launcher warm-ups and cold-starts.

---

## 🛠️ Project Architecture

```
com.kuxlauncher
│
├── drawer                      # App Drawer controllers, states & overlays
│   ├── AppDrawerFragment.kt    # Main drawer layout coordinator fragment
│   ├── DrawerStateManager.kt   # State machine (CLOSED, DRAGGING, ANIMATING, OPENED)
│   ├── DrawerGestureController.kt # Touch finger-tracking drag gesture parser
│   ├── DrawerAnimationHelper.kt # Coordinates physics-based open/close slides
│   └── DrawerSearchManager.kt  # Connects EditText changes with debounced search
│
├── search                      # Real-time search query matching components
│   ├── SearchViewModel.kt      # Asynchronously delegates query filters on background contexts
│   ├── SearchIndexer.kt        # Precomputes normalized indices for instant filtering
│   ├── SearchAdapter.kt        # RecyclerView adapter utilizing text highlights
│   └── SearchHighlighter.kt    # Spans bold, accent color styling over matched substrings
│
├── cache                       # Fast caching components
│   ├── IconCache.kt            # Thread-safe LruCache for in-memory app icons
│   └── AppInfoCache.kt         # Stores list arrays returned by PackageManager
│
├── gesture                     # Home screen gestures & trackers
│   ├── SwipeGestureDetector.kt  # Detects vertical flick gestures on desktop empty space
│   ├── VelocityTrackerHelper.kt # Tracks scroll speed and flick velocities
│   └── WorkspaceDragListener.kt # Intercepts shortcut moves, folder merges & removals
│
├── animation                   # Spring physics & visual depth effects
│   ├── SpringAnimationHelper.kt # Handles android.dynamicanimation spring structures
│   └── DrawerTransitionHelper.kt# Updates scales, translations, and alphas dynamically
│
├── utils                       # Universal helpers
│   ├── BlurHelper.kt           # Generates dark premium glassmorphism shapes
│   ├── DebounceHelper.kt       # Coroutine debounce processor
│   └── KeyboardUtils.kt        # Robust soft input focus coordinator
│
├── view                        # UI Activities
│   └── MainActivity.kt         # Main container orchestrating desktops, folders & drawer
│
└── viewmodel                   # MVVM models
    └── LauncherViewModel.kt    # Drives persistent shortcuts, dock & folder databases
```

---

## 🚀 Building & Running

### Prerequisites
- **JDK 17** or higher
- **Android SDK** API level 24 (Android 7.0 Nougat) or higher
- **Android Studio** (Koala or newer recommended)

### Build the Project
Use the Gradle wrapper to build the debug assembly:
```bash
./gradlew assembleDebug
```

### Running Unit Tests
Execute the local JVM test suite to assert indexers, caches, and highlighters:
```bash
./gradlew testDebugUnitTest
```

---

## 📜 Environment Configurations

Kux Launcher includes template environment configurations in the root directory:
- **[.gitignore](file:///l:/Launcher/.gitignore)**: Configured to ignore JVM builds, Kotlin compiler targets, local user properties, IDE specific metadata, and credentials.
- **[.env](file:///l:/Launcher/.env)**: Stores build-time API parameters and local credentials safely (ignored from git tracking).
