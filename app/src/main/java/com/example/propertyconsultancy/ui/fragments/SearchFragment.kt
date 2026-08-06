package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.cache.CategoryCache
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.example.propertyconsultancy.ui.dialogs.SelectionDialogFragment
import com.example.propertyconsultancy.ui.viewmodels.SearchViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private lateinit var layoutSearchSummary: View
    private lateinit var tvSearchSummary: TextView
    private lateinit var btnEditFilters: View
    private lateinit var layoutMainFilters: View
    
    private lateinit var etSearchCity: AutoCompleteTextView
    private lateinit var btnDoSearch: com.google.android.material.button.MaterialButton
    private lateinit var tvToggleFilters: TextView
    private lateinit var btnClearFilters: TextView
    private lateinit var layoutFold1: View
    private lateinit var tvToggleFold2: TextView
    private lateinit var layoutFold2: View
    
    private lateinit var etMinPrice: EditText
    private lateinit var etMaxPrice: EditText
    private lateinit var etBedrooms: EditText
    private lateinit var etBathrooms: EditText
    
    private lateinit var etFilterFloor: MaterialAutoCompleteTextView
    private lateinit var etFilterFacing: MaterialAutoCompleteTextView
    private lateinit var etFilterRoadSize: MaterialAutoCompleteTextView
    private lateinit var etFilterProType: MaterialAutoCompleteTextView
    
    private lateinit var layoutTopFilters: View
    private lateinit var tvPriceFilterHint: TextView
    private lateinit var tvBedsFilterHint: TextView
    
    private lateinit var chipGroupFavCities: ChipGroup
    private lateinit var searchProgress: LinearProgressIndicator
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvEmptyState: TextView
    
    private lateinit var layoutPagination: View
    private lateinit var layoutPageNumbers: LinearLayout
    
    private var isInitialLoad = true
    
    private lateinit var viewModel: SearchViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var propertyAdapter: com.example.propertyconsultancy.ui.adapters.SearchPropertyAdapter
    private var allCities: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.updateTitle("Listings")
        
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        sessionManager = SessionManager(requireContext())
        
        initViews(view)
        loadSavedFilters()
        restoreState()
        setupFilterToggle()
        setupSelectionInputs()
        fetchCities()

        // Only auto-search if we have a city but NO results yet (initial entry)
        // If we are coming BACK from details, searchResults will already be populated.
        if (viewModel.lastSearchCity.isNotEmpty() && viewModel.searchResults.isEmpty()) {
            performSearch(viewModel.lastSearchCity)
        }
        
        btnDoSearch.setOnClickListener {
            val city = etSearchCity.text.toString().trim()
            if (city.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a city", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.currentPage = 0 
            updateViewModelFromUI()
            viewModel.lastSearchCity = city // Update city in ViewModel
            saveFiltersToPersistence()
            performSearch(city)
        }
        
        btnEditFilters.setOnClickListener {
            viewModel.isMainFilterVisible = true
            updateFoldVisibility()
        }

        btnClearFilters.setOnClickListener {
            clearAllFilters()
        }
    }

    private fun initViews(view: View) {
        layoutSearchSummary = view.findViewById(R.id.layoutSearchSummary)
        tvSearchSummary = view.findViewById(R.id.tvSearchSummary)
        btnEditFilters = view.findViewById(R.id.btnEditFilters)
        layoutMainFilters = view.findViewById(R.id.layoutMainFilters)
        
        etSearchCity = view.findViewById(R.id.etSearchCity)
        btnDoSearch = view.findViewById(R.id.btnDoSearch)
        tvToggleFilters = view.findViewById(R.id.tvToggleFilters)
        btnClearFilters = view.findViewById(R.id.btnClearFilters)
        layoutFold1 = view.findViewById(R.id.layoutFold1)
        tvToggleFold2 = view.findViewById(R.id.tvToggleFold2)
        layoutFold2 = view.findViewById(R.id.layoutFold2)
        
        etMinPrice = view.findViewById(R.id.etMinPrice)
        etMaxPrice = view.findViewById(R.id.etMaxPrice)
        etBedrooms = view.findViewById(R.id.etBedrooms)
        etBathrooms = view.findViewById(R.id.etBathrooms)
        
        etFilterFloor = view.findViewById(R.id.etFilterFloor)
        etFilterFacing = view.findViewById(R.id.etFilterFacing)
        etFilterRoadSize = view.findViewById(R.id.etFilterRoadSize)
        etFilterProType = view.findViewById(R.id.etFilterProType)
        
        layoutTopFilters = view.findViewById(R.id.layoutTopFilters)
        tvPriceFilterHint = view.findViewById(R.id.tvPriceFilterHint)
        tvBedsFilterHint = view.findViewById(R.id.tvBedsFilterHint)
        
        chipGroupFavCities = view.findViewById(R.id.chipGroupFavCities)
        searchProgress = view.findViewById(R.id.searchProgress)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        layoutPagination = view.findViewById(R.id.layoutPagination)
        layoutPageNumbers = view.findViewById(R.id.layoutPageNumbers)

        rvSearchResults.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        propertyAdapter = com.example.propertyconsultancy.ui.adapters.SearchPropertyAdapter(emptyList(), onItemClick = { property, img, title ->
            viewModel.lastClickedPosition = viewModel.searchResults.indexOf(property)
            (activity as? MainActivity)?.openPropertyExplore(property, img, title)
        }, onFilterClick = { type, value ->
            handleQuickFilter(type, value)
        }, onChatClick = { property ->
            openChat(property)
        })
        rvSearchResults.adapter = propertyAdapter
        
        setupSwipeGestures()

        postponeEnterTransition()
        rvSearchResults.viewTreeObserver.addOnPreDrawListener {
            startPostponedEnterTransition()
            true
        }
        
        val favs = listOf("Bhilwara", "Nagpur", "Mumbai", "Pune", "Delhi", "Bangalore")
        favs.forEach { city ->
            val chip = Chip(requireContext())
            chip.text = city
            chip.setOnClickListener { 
                viewModel.currentPage = 0
                updateViewModelFromUI() // Sync other filters before switching city
                viewModel.lastSearchCity = city
                viewModel.searchResults = emptyList() // Clear old results
                viewModel.totalCount = 0
                propertyAdapter.updateData(emptyList())
                
                etSearchCity.setText(city)
                saveFiltersToPersistence()
                performSearch(city)
            }
            chipGroupFavCities.addView(chip)
        }
    }

    private fun loadSavedFilters() {
        val filters = sessionManager.getSearchFilters()
        if (filters.isEmpty()) return
        
        viewModel.lastSearchCity = filters["lastSearchCity"] as? String ?: ""
        viewModel.minPrice = (filters["minPrice"] as? Float)?.toDouble()
        viewModel.maxPrice = (filters["maxPrice"] as? Float)?.toDouble()
        viewModel.bedrooms = filters["bedrooms"] as? Int
        viewModel.bathrooms = (filters["bathrooms"] as? Float)?.toDouble()
        
        val gson = Gson()
        val listType = object : TypeToken<List<Int>>() {}.type
        viewModel.selectedFloorIds = gson.fromJson(filters["selectedFloorIds"] as? String ?: "[]", listType)
        viewModel.selectedFacingIds = gson.fromJson(filters["selectedFacingIds"] as? String ?: "[]", listType)
        viewModel.selectedRoadSizeIds = gson.fromJson(filters["selectedRoadSizeIds"] as? String ?: "[]", listType)
        viewModel.selectedProTypeIds = gson.fromJson(filters["selectedProTypeIds"] as? String ?: "[]", listType)
    }

    private fun saveFiltersToPersistence() {
        val filters = mutableMapOf<String, Any?>()
        filters["lastSearchCity"] = viewModel.lastSearchCity
        filters["minPrice"] = viewModel.minPrice
        filters["maxPrice"] = viewModel.maxPrice
        filters["bedrooms"] = viewModel.bedrooms
        filters["bathrooms"] = viewModel.bathrooms
        filters["selectedFloorIds"] = viewModel.selectedFloorIds
        filters["selectedFacingIds"] = viewModel.selectedFacingIds
        filters["selectedRoadSizeIds"] = viewModel.selectedRoadSizeIds
        filters["selectedProTypeIds"] = viewModel.selectedProTypeIds
        sessionManager.saveSearchFilters(filters)
    }

    private fun updateViewModelFromUI() {
        viewModel.minPrice = etMinPrice.text.toString().toDoubleOrNull()
        viewModel.maxPrice = etMaxPrice.text.toString().toDoubleOrNull()
        viewModel.bedrooms = etBedrooms.text.toString().toIntOrNull()
        viewModel.bathrooms = etBathrooms.text.toString().toDoubleOrNull()
        
        Log.d("[Search]", "ViewModel Updated from UI -> BHK: ${viewModel.bedrooms}, Price: ${viewModel.minPrice}-${viewModel.maxPrice}")
    }

    private fun clearAllFilters() {
        sessionManager.clearSearchFilters()
        viewModel.lastSearchCity = ""
        viewModel.minPrice = null
        viewModel.maxPrice = null
        viewModel.bedrooms = null
        viewModel.bathrooms = null
        viewModel.selectedFloorIds = emptyList()
        viewModel.selectedFacingIds = emptyList()
        viewModel.selectedRoadSizeIds = emptyList()
        viewModel.selectedProTypeIds = emptyList()
        viewModel.currentPage = 0
        viewModel.searchResults = emptyList()
        viewModel.totalCount = 0
        
        etSearchCity.setText("")
        etMinPrice.setText("")
        etMaxPrice.setText("")
        etBedrooms.setText("")
        etBathrooms.setText("")
        etFilterFloor.setText("")
        etFilterFacing.setText("")
        etFilterRoadSize.setText("")
        etFilterProType.setText("")
        
        restoreState()
        propertyAdapter.updateData(emptyList())
    }

    private fun restoreState() {
        if (viewModel.lastSearchCity.isNotEmpty()) {
            etSearchCity.setText(viewModel.lastSearchCity)
            propertyAdapter.updateData(viewModel.searchResults)
            tvEmptyState.visibility = if (viewModel.searchResults.isEmpty()) View.VISIBLE else View.GONE
            updatePaginationUI(sessionManager.getPageSize())
            
            if (viewModel.lastClickedPosition != -1) {
                rvSearchResults.scrollToPosition(viewModel.lastClickedPosition)
            }
        }
        
        etMinPrice.setText(viewModel.minPrice?.toString() ?: "")
        etMaxPrice.setText(viewModel.maxPrice?.toString() ?: "")
        etBedrooms.setText(viewModel.bedrooms?.toString() ?: "")
        etBathrooms.setText(viewModel.bathrooms?.toString() ?: "")
        
        val categories = CategoryCache.getCategories(requireContext())
        fun setSummary(ids: List<Int>, group: String, target: EditText) {
            if (ids.isEmpty()) { target.setText(""); return }
            val options = categories?.find { it.name.contains(group, true) }?.options
            val summary = options?.filter { ids.contains(it.categoryId) }?.joinToString(", ") { it.option }
            target.setText(summary ?: "")
        }
        setSummary(viewModel.selectedFloorIds, "Floor", etFilterFloor)
        setSummary(viewModel.selectedFacingIds, "Facing", etFilterFacing)
        setSummary(viewModel.selectedRoadSizeIds, "Road", etFilterRoadSize)
        setSummary(viewModel.selectedProTypeIds, "Type", etFilterProType)

        updateFoldVisibility()
        updateFilterHints()
    }

    private fun setupFilterToggle() {
        tvToggleFilters.setOnClickListener {
            viewModel.isFold1Visible = !viewModel.isFold1Visible
            if (!viewModel.isFold1Visible) viewModel.isFold2Visible = false
            updateFoldVisibility()
            updateFilterHints()
        }
        
        tvToggleFold2.setOnClickListener {
            viewModel.isFold2Visible = !viewModel.isFold2Visible
            updateFoldVisibility()
        }
    }

    private fun updateFoldVisibility() {
        layoutMainFilters.visibility = if (viewModel.isMainFilterVisible) View.VISIBLE else View.GONE
        layoutSearchSummary.visibility = if (viewModel.isMainFilterVisible) View.GONE else View.VISIBLE
        
        layoutFold1.visibility = if (viewModel.isFold1Visible && viewModel.isMainFilterVisible) View.VISIBLE else View.GONE
        layoutFold2.visibility = if (viewModel.isFold2Visible && viewModel.isMainFilterVisible) View.VISIBLE else View.GONE
        
        layoutTopFilters.visibility = if (!viewModel.isFold1Visible && viewModel.isMainFilterVisible) View.VISIBLE else View.GONE
        tvToggleFilters.text = if (viewModel.isFold1Visible) "Hide Filters ▲" else "Show Filters ▼"
        tvToggleFold2.text = if (viewModel.isFold2Visible) "Basic Only ▲" else "Advanced Selection ▼"
        
        if (!viewModel.isMainFilterVisible) {
            tvSearchSummary.text = "Search in ${viewModel.lastSearchCity} (${viewModel.totalCount})"
        }
    }

    private fun updateFilterHints() {
        val min = etMinPrice.text.toString()
        val max = etMaxPrice.text.toString()
        val beds = etBedrooms.text.toString()
        tvPriceFilterHint.text = if (min.isEmpty() && max.isEmpty()) "Any Price" else "₹$min - ₹$max"
        tvBedsFilterHint.text = if (beds.isEmpty()) "Any Beds" else "$beds BHK"
    }

    private fun setupSelectionInputs() {
        val categories = CategoryCache.getCategories(requireContext()) ?: return
        fun showMultiDialog(title: String, groupName: String, target: EditText, currentIds: List<Int>, onUpdate: (List<Int>) -> Unit) {
            val options = categories.find { it.name.contains(groupName, true) }?.options ?: emptyList()
            SelectionDialogFragment(title, options, isMultiSelect = true, initialSelectedIds = currentIds, onSelected = {}, onMultiSelected = { ids, summary ->
                onUpdate(ids)
                target.setText(summary)
                saveFiltersToPersistence()
            }).show(parentFragmentManager, "MultiSelectionDialog")
        }
        etFilterFloor.setOnClickListener { showMultiDialog("Select Floors", "Floor", etFilterFloor, viewModel.selectedFloorIds) { viewModel.selectedFloorIds = it } }
        etFilterFacing.setOnClickListener { showMultiDialog("Select Facing", "Facing", etFilterFacing, viewModel.selectedFacingIds) { viewModel.selectedFacingIds = it } }
        etFilterRoadSize.setOnClickListener { showMultiDialog("Select Road Size", "Road", etFilterRoadSize, viewModel.selectedRoadSizeIds) { viewModel.selectedRoadSizeIds = it } }
        etFilterProType.setOnClickListener { showMultiDialog("Select Property Type", "Type", etFilterProType, viewModel.selectedProTypeIds) { viewModel.selectedProTypeIds = it } }
    }

    private fun fetchCities() {
        lifecycleScope.launch {
            try {
                allCities = listOf("Bhilwara", "Nagpur", "Mumbai", "Pune", "Delhi", "Bangalore", "Hyderabad")
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, allCities)
                etSearchCity.setAdapter(adapter)
            } catch (e: Exception) {}
        }
    }

    private fun performSearch(city: String) {
        if (city.isEmpty()) return
        
        viewModel.lastSearchCity = city
        viewModel.lastClickedPosition = -1
        
        // Detailed logging for filters
        Log.d("[Search]", "Performing Search in $city")
        Log.d("[Search]", "Filters -> Price: ${viewModel.minPrice}-${viewModel.maxPrice}, BHK: ${viewModel.bedrooms}, Baths: ${viewModel.bathrooms}")
        
        // Reset current results to avoid confusion while loading
        if (viewModel.currentPage == 0) {
            viewModel.searchResults = emptyList()
            propertyAdapter.updateData(emptyList())
            viewModel.totalCount = 0
        }
        
        if (!viewModel.isMainFilterVisible) {
            tvSearchSummary.text = "Searching in $city..."
        }
        
        // Only hide filters if we are NOT on a pagination change
        // But for a fresh search, we hide them
        if (viewModel.isMainFilterVisible && viewModel.currentPage == 0) {
            viewModel.isMainFilterVisible = false
            viewModel.isFold1Visible = false
            viewModel.isFold2Visible = false
            updateFoldVisibility()
        }
        
        updateFilterHints()
        searchProgress.visibility = View.VISIBLE

        val pageSize = sessionManager.getPageSize()
        val offset = viewModel.currentPage * pageSize
        
        Log.d("[Pagination]", "Requesting City: $city, Page: ${viewModel.currentPage}, Limit: $pageSize, Offset: $offset, BHK: ${viewModel.bedrooms}")
        
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getProperties(
                    city = city,
                    minPrice = viewModel.minPrice,
                    maxPrice = viewModel.maxPrice,
                    bedrooms = viewModel.bedrooms,
                    bathrooms = viewModel.bathrooms,
                    floorIds = if (viewModel.selectedFloorIds.isEmpty()) null else viewModel.selectedFloorIds.joinToString(","),
                    facingIds = if (viewModel.selectedFacingIds.isEmpty()) null else viewModel.selectedFacingIds.joinToString(","),
                    roadsizeIds = if (viewModel.selectedRoadSizeIds.isEmpty()) null else viewModel.selectedRoadSizeIds.joinToString(","),
                    protypeIds = if (viewModel.selectedProTypeIds.isEmpty()) null else viewModel.selectedProTypeIds.joinToString(","),
                    limit = pageSize,
                    offset = offset
                )
                
                Log.d("[Search]", "Response Status: ${response.status}, Count: ${response.count}, Data Size: ${response.data?.size}")
                
                if (response.status == "success") {
                    val properties = response.data ?: emptyList()
                    viewModel.searchResults = properties
                    
                    // CRITICAL: Ensure we use the total matching count from the server, 
                    // not just the number of properties in this specific page.
                    viewModel.totalCount = response.count ?: properties.size
                    
                    Log.d("[Pagination]", "Success: Got ${properties.size} items. Total matching in DB: ${viewModel.totalCount}")
                    
                    propertyAdapter.updateData(properties)
                    rvSearchResults.scrollToPosition(0)
                    
                    tvEmptyState.visibility = if (properties.isEmpty()) View.VISIBLE else View.GONE
                    updatePaginationUI(pageSize)
                    updateFoldVisibility() 
                } else {
                    Toast.makeText(requireContext(), response.message ?: "Search failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("[Search]", "Error: ${e.message}")
                Toast.makeText(requireContext(), "Error fetching properties", Toast.LENGTH_SHORT).show()
            } finally {
                searchProgress.visibility = View.GONE
            }
        }
    }

    private fun setupSwipeGestures() {
        val gestureDetector = android.view.GestureDetector(requireContext(), object : android.view.GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            // Left swipe -> Next Page
                            onSwipeLeft()
                        } else {
                            // Right swipe -> Previous Page
                            onSwipeRight()
                        }
                        return true
                    }
                }
                return false
            }
        })

        rvSearchResults.setOnTouchListener { _, event -> 
            gestureDetector.onTouchEvent(event)
            false // Return false to allow RV to handle its own scrolls
        }
    }

    private fun onSwipeLeft() {
        val pageSize = sessionManager.getPageSize()
        val totalPages = Math.ceil(viewModel.totalCount.toDouble() / pageSize).toInt()
        if (viewModel.currentPage < totalPages - 1) {
            viewModel.currentPage++
            updateViewModelFromUI()
            performSearch(viewModel.lastSearchCity)
            Toast.makeText(requireContext(), "Next Page", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onSwipeRight() {
        if (viewModel.currentPage > 0) {
            viewModel.currentPage--
            updateViewModelFromUI()
            performSearch(viewModel.lastSearchCity)
            Toast.makeText(requireContext(), "Previous Page", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePaginationUI(pageSize: Int) {
        val totalPages = Math.ceil(viewModel.totalCount.toDouble() / pageSize).toInt()
        if (totalPages <= 1) {
            layoutPagination.visibility = View.GONE
        } else {
            layoutPagination.visibility = View.VISIBLE
            populatePageNumbers(totalPages)
        }
    }

    private fun populatePageNumbers(totalPages: Int) {
        layoutPageNumbers.removeAllViews()
        val context = requireContext()
        val density = resources.displayMetrics.density
        val size = (30 * density).toInt() // Smaller, round size
        val margin = (4 * density).toInt()

        for (i in 0 until totalPages) {
            val tv = TextView(context)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, (8 * density).toInt(), margin, 0) // Touches bottom
            tv.layoutParams = params
            tv.gravity = android.view.Gravity.CENTER
            tv.text = (i + 1).toString()
            tv.textSize = 12f // Smaller font
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL // Round

            if (i == viewModel.currentPage) {
                // Highlighted: Opaque with theme color
                drawable.setColor(ContextCompat.getColor(context, R.color.modern_primary))
                drawable.setStroke(0, android.graphics.Color.TRANSPARENT)
                tv.setTextColor(android.graphics.Color.WHITE)
            } else {
                // Others: Fully transparent with primary border
                drawable.setColor(android.graphics.Color.TRANSPARENT)
                drawable.setStroke((1 * density).toInt(), ContextCompat.getColor(context, R.color.modern_primary))
                tv.setTextColor(ContextCompat.getColor(context, R.color.modern_primary))
            }
            
            tv.background = drawable

            tv.setOnClickListener {
                if (viewModel.currentPage != i) {
                    viewModel.currentPage = i
                    updateViewModelFromUI()
                    performSearch(viewModel.lastSearchCity)
                }
            }
            layoutPageNumbers.addView(tv)
        }
    }

    private fun handleQuickFilter(type: String, value: Any) {
        val title = "Quick Filter"
        val message = "Filter by $value $type?\n\n"
        val explanation = "Tapping an attribute in the list allows you to instantly narrow down results to only properties matching that specific value (e.g., only this BHK or price range)."

        val spannable = android.text.SpannableString(message + explanation)
        spannable.setSpan(android.text.style.RelativeSizeSpan(0.85f), message.length, spannable.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.DKGRAY), message.length, spannable.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), message.length, spannable.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title).setMessage(spannable)
            .setPositiveButton("Apply Only This") { _, _ ->
                viewModel.isMainFilterVisible = false
                // For "Apply Only This", we clear other filters and set this one
                clearAllFiltersExcept(type, value)
                
                viewModel.currentPage = 0
                saveFiltersToPersistence()
                performSearch(viewModel.lastSearchCity)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun clearAllFiltersExcept(type: String, value: Any) {
        // Keep the city
        val currentCity = viewModel.lastSearchCity
        
        // Clear all
        viewModel.minPrice = null
        viewModel.maxPrice = null
        viewModel.bedrooms = null
        viewModel.bathrooms = null
        viewModel.selectedFloorIds = emptyList()
        viewModel.selectedFacingIds = emptyList()
        viewModel.selectedRoadSizeIds = emptyList()
        viewModel.selectedProTypeIds = emptyList()
        
        etMinPrice.setText("")
        etMaxPrice.setText("")
        etBedrooms.setText("")
        etBathrooms.setText("")
        etFilterFloor.setText("")
        etFilterFacing.setText("")
        etFilterRoadSize.setText("")
        etFilterProType.setText("")

        // Set the one we want
        if (type == "BHK") { 
            val bhk = value as Int
            viewModel.bedrooms = bhk
            etBedrooms.setText(bhk.toString())
        } else if (type == "Price") {
            val price = value as Double
            viewModel.maxPrice = price
            etMaxPrice.setText(price.toString())
        }
        
        viewModel.lastSearchCity = currentCity
        etSearchCity.setText(currentCity)
    }

    private fun openChat(property: com.example.propertyconsultancy.data.dto.PropertyDTO) {
        (activity as? MainActivity)?.openChat(property)
    }
}
