package com.kuxlauncher.drawer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.kuxlauncher.R
import com.kuxlauncher.data.AppModel
import com.kuxlauncher.databinding.FragmentAppDrawerBinding
import com.kuxlauncher.search.SearchAdapter
import com.kuxlauncher.search.SearchViewModel
import com.kuxlauncher.utils.BlurHelper
import com.kuxlauncher.utils.KeyboardUtils
import com.kuxlauncher.view.MainActivity
import java.util.Locale

/**
 * Full-screen App Drawer Fragment coordinating high-performance searchable lists,
 * gestural keyboard focus, spring transition updates, and alphabetical index sidebars.
 */
class AppDrawerFragment : Fragment() {

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var searchAdapter: SearchAdapter

    private lateinit var stateManager: DrawerStateManager
    private lateinit var animationHelper: DrawerAnimationHelper
    private lateinit var gestureController: DrawerGestureController
    private lateinit var searchManager: DrawerSearchManager

    // Communication callback when the drawer state changes
    var onStateChangedCallback: ((DrawerStateManager.DrawerState) -> Unit)? = null
    // Callback when an app is long clicked to initiate home screen drag and drop
    var onAppLongClickCallback: ((AppModel, View) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        super.onViewCreated(view, savedInstanceState)

        // Apply glassmorphic visual background
        BlurHelper.applyGlassmorphism(binding.root, cornerRadiusPx = 60f)

        // Initialize state machine
        stateManager = DrawerStateManager { state ->
            onStateChangedCallback?.invoke(state)
            handleStateSpecificActions(state)
        }

        // Initialize animation helper and touch gesture listeners
        val workspaceView = requireActivity().findViewById<View>(R.id.workspaceContainer)
        animationHelper = DrawerAnimationHelper(binding.root, workspaceView, stateManager)
        
        gestureController = DrawerGestureController(binding.root, animationHelper, stateManager)
        binding.root.setOnTouchListener(gestureController)
        binding.dragHandle.setOnTouchListener(gestureController)

        // Setup search adapter with asynchronous icon bindings
        setupRecyclerView()

        // Setup search managers
        searchManager = DrawerSearchManager(binding.searchEditText, searchViewModel, viewLifecycleOwner)

        // Setup Fast Alphabetical Sidebar Indexer
        setupAlphabeticalSidebar()

        // Observe search data
        observeSearch()

        // Warm up and build the search index in background
        searchViewModel.initializeIndex()

        // Postpone positioning drawer offscreen until measurements are complete
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                // Slide drawer out of screen bounds to closed position initially
                animationHelper.setTranslationYDirect(view.height.toFloat())
            }
        })
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter(
            packageManager = requireContext().packageManager,
            onAppClick = { app ->
                launchApp(app)
            },
            onAppLongClick = { app, itemView ->
                // Hide search keyboard instantly
                KeyboardUtils.hideKeyboard(binding.searchEditText)
                
                // Instantly collapse/hide drawer view to reveal the home screen workspace grid
                animationHelper.setTranslationYDirect(binding.root.height.toFloat())
                stateManager.setState(DrawerStateManager.DrawerState.CLOSED)
                
                // Delegate the drag operation to MainActivity callback
                onAppLongClickCallback?.invoke(app, itemView)
                true
            }
        )

        binding.drawerRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = searchAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupAlphabeticalSidebar() {
        val alphabet = ('A'..'Z').toList() + '#'
        binding.indexBarContainer.removeAllViews()

        for (char in alphabet) {
            val tv = TextView(requireContext()).apply {
                text = char.toString()
                textSize = 9f
                setTextColor(ContextCompat.getColor(context, R.color.text_white_low_emphasis))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 2, 0, 2)
                isClickable = true
                isFocusable = true
                
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                        scrollToSection(char)
                        true
                    } else {
                        false
                    }
                }
            }
            binding.indexBarContainer.addView(tv)
        }
    }

    private fun scrollToSection(char: Char) {
        val apps = searchAdapter.currentList
        val targetChar = char.lowercase(Locale.getDefault())

        val index = apps.indexOfFirst { app ->
            if (char == '#') {
                val firstChar = app.label.firstOrNull() ?: ' '
                !firstChar.isLetter()
            } else {
                app.label.lowercase(Locale.getDefault()).startsWith(targetChar)
            }
        }

        if (index != -1) {
            val layoutManager = binding.drawerRecyclerView.layoutManager as? GridLayoutManager
            layoutManager?.scrollToPositionWithOffset(index, 0)
        }
    }

    private fun observeSearch() {
        searchViewModel.searchResults.observe(viewLifecycleOwner) { filteredApps ->
            searchAdapter.submitList(filteredApps)
            binding.emptyStateView.isVisible = filteredApps.isEmpty()
            binding.drawerRecyclerView.isVisible = filteredApps.isNotEmpty()
        }

        searchViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            searchAdapter.updateQuery(query)
        }
    }

    private fun handleStateSpecificActions(state: DrawerStateManager.DrawerState) {
        when (state) {
            DrawerStateManager.DrawerState.OPENED -> {
                // Automatically request focus and display keyboard for real-time search
                KeyboardUtils.showKeyboard(binding.searchEditText)
            }
            DrawerStateManager.DrawerState.CLOSED -> {
                // Instantly clean search filter and dismiss keyboard
                searchManager.clearSearch()
                KeyboardUtils.hideKeyboard(binding.searchEditText)
            }
            else -> {}
        }
    }

    /**
     * Public method to slide open the drawer.
     */
    fun open() {
        animationHelper.animateOpen()
    }

    /**
     * Public method to slide close the drawer.
     */
    fun close() {
        animationHelper.animateClose()
    }

    /**
     * Checks if the drawer is currently open.
     */
    fun isOpened(): Boolean {
        return stateManager.isOpened
    }

    /**
     * Checks if the drawer is currently closed.
     */
    fun isClosed(): Boolean {
        return stateManager.isClosed
    }

    private fun launchApp(app: AppModel) {
        val activity = requireActivity() as? MainActivity
        activity?.launchAppDirect(app.packageName, app.className, app.label)
    }

    override fun onDestroyView() {
        searchManager.onDestroy()
        _binding = null
        super.onDestroyView()
    }
}
