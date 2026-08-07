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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.propertyconsultancy.utils.UrlUtils
import coil3.load
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.MapStyleOptions
import android.graphics.PorterDuff
import java.util.Locale

class SearchFragment : Fragment(), OnMapReadyCallback {

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
    private lateinit var layoutMapContainer: View
    
    private lateinit var cardMapProperty: View
    private lateinit var ivMapProperty: ImageView
    private lateinit var tvMapPropertyTitle: TextView
    private lateinit var tvMapPropertyPrice: TextView
    private lateinit var tvMapPropertyType: TextView
    private lateinit var tvMapPropertyBath: TextView
    private lateinit var tvMapPropertyArea: TextView
    private lateinit var tvMapPropertyLocation: TextView
    private lateinit var tvMapPropertyBhk: TextView
    private lateinit var tvMapPropertyFacing: TextView
    private lateinit var btnMapExplore: com.google.android.material.button.MaterialButton
    private lateinit var btnMapCloseCard: View
    
    private lateinit var tvPageSizeInfo: TextView
    private lateinit var tvPageSizeInfoSummary: TextView
    private lateinit var btnAiFilter: View
    private lateinit var btnToggleViewMode: View
    private lateinit var ivFilterCity: ImageView
    private lateinit var ivFilterMinPrice: ImageView
    private lateinit var ivFilterMaxPrice: ImageView
    private lateinit var ivFilterBedrooms: ImageView
    private lateinit var ivFilterBathrooms: ImageView
    private lateinit var ivFilterFloor: ImageView
    private lateinit var ivFilterFacing: ImageView
    private lateinit var ivFilterRoadSize: ImageView
    private lateinit var ivFilterProType: ImageView
    private lateinit var ivSwipeHintLeft: ImageView
    private lateinit var ivSwipeHintRight: android.widget.ImageView
    private lateinit var ivHudHintCenter: android.widget.ImageView
    private lateinit var ivHudHintBottom: android.widget.ImageView
    
    private enum class HintPointer { UP, DOWN, LEFT, RIGHT, NONE }

    private var isMapView = false
    private var googleMap: GoogleMap? = null
    private val mapMarkers = mutableListOf<com.google.android.gms.maps.model.Marker>()
    private val captionCycleHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var captionCycleRunnable: Runnable? = null
    
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
        initMap()
        
        // Explicitly set initial visibility based on isMapView (Default: List)
        updateViewModeVisibility()
        
        loadSavedFilters()
        restoreState()
        setupFilterToggle()
        setupSelectionInputs()
        fetchCities()

        if (viewModel.lastSearchCity.isNotEmpty() && viewModel.searchResults.isEmpty()) {
            performSearch(viewModel.lastSearchCity)
        }
        
        btnDoSearch.setOnClickListener {
            val city = etSearchCity.text.toString().trim()
            if (city.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a city", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showHintPopup("Initiating Search in $city")
            viewModel.currentPage = 0 
            updateViewModelFromUI()
            viewModel.lastSearchCity = city
            saveFiltersToPersistence()
            updateFilterIndicators()
            performSearch(city)
        }
        
        btnEditFilters.setOnClickListener {
            showHintAtView(it, "Opening Advanced Filters", HintPointer.DOWN)
            viewModel.isMainFilterVisible = true
            updateFoldVisibility()
        }

        btnAiFilter.setOnClickListener {
            showHintAtView(it, "AI Assistant Activated", HintPointer.DOWN)
            com.example.propertyconsultancy.ui.dialogs.AiFilterDialog { requirement ->
                parseAiRequirement(requirement)
            }.show(parentFragmentManager, "AiFilterDialog")
        }

        btnClearFilters.setOnClickListener {
            showHintPopup("Filters Cleared")
            clearAllFilters()
        }

        btnToggleViewMode.setOnClickListener {
            toggleViewMode()
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
        layoutMapContainer = view.findViewById(R.id.layoutMapContainer)
        
        cardMapProperty = view.findViewById(R.id.cardMapProperty)
        ivMapProperty = view.findViewById(R.id.ivMapProperty)
        tvMapPropertyTitle = view.findViewById(R.id.tvMapPropertyTitle)
        tvMapPropertyPrice = view.findViewById(R.id.tvMapPropertyPrice)
        tvMapPropertyType = view.findViewById(R.id.tvMapPropertyType)
        tvMapPropertyBath = view.findViewById(R.id.tvMapPropertyBath)
        tvMapPropertyArea = view.findViewById(R.id.tvMapPropertyArea)
        tvMapPropertyLocation = view.findViewById(R.id.tvMapPropertyLocation)
        tvMapPropertyBhk = view.findViewById(R.id.tvMapPropertyBhk)
        tvMapPropertyFacing = view.findViewById(R.id.tvMapPropertyFacing)
        btnMapExplore = view.findViewById(R.id.btnMapExplore)
        btnMapCloseCard = view.findViewById(R.id.btnMapCloseCard)
        
        btnMapCloseCard.setOnClickListener {
            viewModel.selectedPropertyOnMap = null
            cardMapProperty.visibility = View.GONE
        }
        
        tvPageSizeInfo = view.findViewById(R.id.tvPageSizeInfo)
        tvPageSizeInfoSummary = view.findViewById(R.id.tvPageSizeInfoSummary)
        btnAiFilter = view.findViewById(R.id.btnAiFilter)
        btnToggleViewMode = view.findViewById(R.id.btnToggleViewMode)
        ivFilterCity = view.findViewById(R.id.ivFilterCity)
        ivFilterMinPrice = view.findViewById(R.id.ivFilterMinPrice)
        ivFilterMaxPrice = view.findViewById(R.id.ivFilterMaxPrice)
        ivFilterBedrooms = view.findViewById(R.id.ivFilterBedrooms)
        ivFilterBathrooms = view.findViewById(R.id.ivFilterBathrooms)
        ivFilterFloor = view.findViewById(R.id.ivFilterFloor)
        ivFilterFacing = view.findViewById(R.id.ivFilterFacing)
        ivFilterRoadSize = view.findViewById(R.id.ivFilterRoadSize)
        ivFilterProType = view.findViewById(R.id.ivFilterProType)
        ivSwipeHintLeft = view.findViewById(R.id.ivSwipeHintLeft)
        ivSwipeHintRight = view.findViewById(R.id.ivSwipeHintRight)
        ivHudHintCenter = view.findViewById(R.id.ivHudHintCenter)
        ivHudHintBottom = view.findViewById(R.id.ivHudHintBottom)
        
        val pageSize = sessionManager.getPageSize()
        val pageSizeText = "[ $pageSize Property Per Page ]"
        tvPageSizeInfo.text = pageSizeText
        tvPageSizeInfoSummary.text = pageSizeText

        rvSearchResults.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        propertyAdapter = com.example.propertyconsultancy.ui.adapters.SearchPropertyAdapter(emptyList(), onItemClick = { property, sharedElements ->
            viewModel.lastClickedPosition = viewModel.searchResults.indexOf(property)
            (activity as? MainActivity)?.openPropertyExplore(property, sharedElements)
        }, onFilterClick = { type, value ->
            handleQuickFilter(type, value)
        }, onChatClick = { property ->
            openChat(property)
        },
        currentCity = viewModel.lastSearchCity,
        currentBhk = viewModel.bedrooms,
        currentMinPrice = viewModel.minPrice,
        currentMaxPrice = viewModel.maxPrice,
        currentProTypeIds = viewModel.selectedProTypeIds
        )
        rvSearchResults.adapter = propertyAdapter
        
        setupSwipeGestures()

        postponeEnterTransition()
        view.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                startPostponedEnterTransition()
                return true
            }
        })
        
        val favs = listOf("Bhilwara", "Nagpur", "Mumbai", "Pune", "Delhi", "Bangalore")
        favs.forEach { city ->
            val chip = Chip(requireContext())
            chip.text = city
            chip.setOnClickListener { 
                viewModel.currentPage = 0
                updateViewModelFromUI()
                viewModel.lastSearchCity = city
                viewModel.searchResults = emptyList()
                viewModel.totalCount = 0
                propertyAdapter.updateData(emptyList())
                updateMapMarkers()
                
                etSearchCity.setText(city)
                saveFiltersToPersistence()
                performSearch(city)
            }
            chipGroupFavCities.addView(chip)
        }
    }

    private fun initMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapResults) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        
        updateMapMarkers()
        startCaptionCycle()
    }

    private fun toggleViewMode() {
        isMapView = !isMapView
        updateViewModeVisibility()
        
        val mode = if (isMapView) "Map" else "List"
        showHintAtView(btnToggleViewMode, "Switched to $mode View", HintPointer.DOWN)
        sessionManager.addActivityLog("View Mode", "Switched to $mode view", "map")
        
        // Re-fetch to handle different page sizes between Map (all) and List (paged)
        if (viewModel.lastSearchCity.isNotEmpty()) {
            performSearch(viewModel.lastSearchCity)
        }
    }

    private fun updateViewModeVisibility() {
        if (isMapView) {
            rvSearchResults.visibility = View.GONE
            layoutMapContainer.visibility = View.VISIBLE
            layoutPagination.visibility = View.GONE
            (btnToggleViewMode as? ImageButton)?.setImageResource(R.drawable.ic_search_modern)
        } else {
            rvSearchResults.visibility = View.VISIBLE
            layoutMapContainer.visibility = View.GONE
            cardMapProperty.visibility = View.GONE // Hide map card in list view
            // Pagination visibility will be updated by updatePaginationUI after search results load
            (btnToggleViewMode as? ImageButton)?.setImageResource(R.drawable.ic_location_pin)
        }
    }

    override fun onDestroyView() {
        captionCycleRunnable?.let { captionCycleHandler.removeCallbacks(it) }
        mapMarkers.forEach { it.remove() }
        mapMarkers.clear()
        super.onDestroyView()
    }

    private fun updateMapMarkers() {
        val map = googleMap ?: return
        mapMarkers.forEach { it.remove() }
        mapMarkers.clear()
        
        val properties = viewModel.searchResults
        if (properties.isEmpty()) return

        var firstPos: LatLng? = null
        properties.forEach { property ->
            val lat = property.latitude
            val lng = property.longitude
            if (lat != null && lng != null) {
                val pos = LatLng(lat, lng)
                if (firstPos == null) firstPos = pos
                
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .icon(getMarkerIcon(R.drawable.ic_hut, null))
                )
                if (marker != null) {
                    marker.tag = property
                    mapMarkers.add(marker)
                }
            }
        }

        firstPos?.let {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 12f))
        }

        map.setOnInfoWindowClickListener { marker ->
            val property = marker.tag as? com.example.propertyconsultancy.data.dto.PropertyDTO
            if (property != null) {
                (activity as? MainActivity)?.openPropertyExplore(property)
            }
        }

        map.setOnMarkerClickListener { marker ->
            val property = marker.tag as? com.example.propertyconsultancy.data.dto.PropertyDTO
            if (property != null) {
                viewModel.selectedPropertyOnMap = property
                showPropertyOnMapCard(property)
                // Returning false allows standard center-on-marker behavior
            }
            false
        }
        
        // Force immediate update with current cycle info
        refreshMarkersWithCaption()
    }

    private fun showPropertyOnMapCard(property: com.example.propertyconsultancy.data.dto.PropertyDTO) {
        val ctx = context ?: return
        tvMapPropertyTitle.text = property.title?.uppercase()
        val formatter = java.text.DecimalFormat("#,###")
        tvMapPropertyPrice.text = "₹ ${formatter.format(property.pricePerMonth ?: 0.0)}"
        
        tvMapPropertyBath.text = "${property.bathrooms?.toInt() ?: 0} Bath"
        tvMapPropertyArea.text = "${property.areaSqft} Sqft"
        tvMapPropertyBhk.text = "${property.bedrooms} BHK"
        
        val locationText = buildString {
            if (!property.addressLine2.isNullOrEmpty()) append("${property.addressLine2}, ")
            append(property.city ?: "")
        }
        tvMapPropertyLocation.text = locationText

        val categories = CategoryCache.getCategories(ctx)
        fun getOptionName(ids: List<Int>?, group: String): String {
            if (ids.isNullOrEmpty()) return "N/A"
            val options = categories?.find { it.name.contains(group, true) }?.options
            return options?.find { it.categoryId == ids.first() }?.option ?: "ID: ${ids.first()}"
        }
        
        tvMapPropertyType.text = getOptionName(property.proTypeId?.let { listOf(it) }, "Type").uppercase()
        tvMapPropertyFacing.text = getOptionName(property.facingId?.let { listOf(it) }, "Facing")
        
        val imageUrl = property.media?.firstOrNull()?.fileUrl ?: property.mediaUrls?.firstOrNull()
        
        ivMapProperty.load(UrlUtils.getPropertyImageUrl(imageUrl) ?: R.drawable.ic_app_logo)
        
        // Prepare Shared Elements
        val sharedElements = mutableMapOf<String, View>()
        val prefixes = listOf("IMAGE", "TITLE", "PRICE", "LOCATION", "BHK", "AREA", "FACING", "BATH", "TYPE")
        val views = listOf(ivMapProperty, tvMapPropertyTitle, tvMapPropertyPrice, tvMapPropertyLocation, tvMapPropertyBhk, tvMapPropertyArea, tvMapPropertyFacing, tvMapPropertyBath, tvMapPropertyType)
        
        views.forEachIndexed { index, view ->
            val name = "property_${prefixes[index].lowercase()}_map"
            view.transitionName = name
            sharedElements[name] = view
        }

        btnMapExplore.setOnClickListener {
            (activity as? MainActivity)?.openPropertyExplore(property, sharedElements)
        }
        
        cardMapProperty.visibility = View.VISIBLE
        
        // Brief animation for pop-up effect
        cardMapProperty.alpha = 0f
        cardMapProperty.translationY = 50f
        cardMapProperty.animate().alpha(1f).translationY(0f).setDuration(300).start()
    }

    private fun startCaptionCycle() {
        captionCycleRunnable = object : Runnable {
            override fun run() {
                if (!isMapView || googleMap == null || mapMarkers.isEmpty()) {
                    captionCycleHandler.postDelayed(this, 2000)
                    return
                }

                // Pick 1-3 random markers to update
                val count = (1..3).random().coerceAtMost(mapMarkers.size)
                val shuffled = mapMarkers.shuffled().take(count)
                
                shuffled.forEach { marker ->
                    val ctx = context ?: return@forEach
                    val property = marker.tag as? com.example.propertyconsultancy.data.dto.PropertyDTO ?: return@forEach
                    val typeIndex = (1..3).random() // 1: Rent, 2: Title, 3: Type
                    val categories = CategoryCache.getCategories(ctx)
                    
                    val caption = when (typeIndex) {
                        1 -> {
                            val formatter = java.text.DecimalFormat("#,###")
                            "₹ ${formatter.format(property.pricePerMonth ?: 0.0)}"
                        }
                        2 -> property.title?.take(15)?.uppercase() ?: ""
                        3 -> {
                            val group = categories?.find { it.name.contains("Type", true) }
                            group?.options?.find { it.categoryId == property.proTypeId }?.option ?: "Property"
                        }
                        else -> null
                    }

                    // Show stylish HUD caption
                    marker.setIcon(getMarkerIcon(R.drawable.ic_hut, caption))
                    
                    // Hide it after a short random duration (2.5 - 4.5 seconds)
                    captionCycleHandler.postDelayed({
                        if (isAdded && isMapView && googleMap != null) {
                            marker.setIcon(getMarkerIcon(R.drawable.ic_hut, null))
                        }
                    }, (2500..4500).random().toLong())
                }

                // Schedule next wave of random pop-ups quickly for a busy HUD feel
                captionCycleHandler.postDelayed(this, (1000..2500).random().toLong())
            }
        }
        captionCycleHandler.postDelayed(captionCycleRunnable!!, 1000)
    }

    private fun refreshMarkersWithCaption() {
        // No longer needed in bulk as we now pop them randomly
        if (!isMapView || googleMap == null) return
        mapMarkers.forEach { it.setIcon(getMarkerIcon(R.drawable.ic_hut, null)) }
    }

    private fun getMarkerIcon(resourceId: Int, caption: String?): BitmapDescriptor? {
        val ctx = context ?: return null
        val drawable = ResourcesCompat.getDrawable(ctx.resources, resourceId, null) ?: return null
        
        // Futuristic HUD Styling - Bigger & Muted
        val accentColor = android.graphics.Color.parseColor("#E53935") // Vivid Red
        val hudLineColor = android.graphics.Color.parseColor("#757575") // Muted Gray
        val textColor = android.graphics.Color.parseColor("#424242") // Darker Gray Text
        
        val paintText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 34f // Increased Size
            color = textColor
            typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        }

        val textWidth = if (caption != null) paintText.measureText(caption) else 0f
        val textHeight = 40f
        val padding = 18f
        
        // Canvas size: large enough for larger callout layout
        val width = 500
        val height = 300
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val centerX = width / 2f
        val bottomY = height - 40f
        
        // 1. Draw Anchor Hut
        drawable.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
        drawable.alpha = 220
        val iconSize = 48 // Bigger Icon
        drawable.setBounds((centerX - iconSize/2).toInt(), (bottomY - iconSize).toInt(), (centerX + iconSize/2).toInt(), bottomY.toInt())
        drawable.draw(canvas)

        if (caption != null) {
            val isRight = (0..1).random() == 0
            val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = hudLineColor
                strokeWidth = 2.5f // Thicker lines
                style = android.graphics.Paint.Style.STROKE
            }
            val nodePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = android.graphics.Paint.Style.FILL
            }

            // 2. Futuristic Callout Geometry (Bigger)
            val startY = bottomY - iconSize - 4
            val vLineLen = 45f
            val dLineX = if (isRight) 70f else -70f
            val dLineY = 40f
            
            val p1 = android.graphics.PointF(centerX, startY)
            val p2 = android.graphics.PointF(centerX, startY - vLineLen)
            val p3 = android.graphics.PointF(centerX + dLineX, startY - vLineLen - dLineY)
            val p4 = android.graphics.PointF(p3.x + (if (isRight) 25 else -25), p3.y)
            
            val path = android.graphics.Path()
            path.moveTo(p1.x, p1.y)
            path.lineTo(p2.x, p2.y)
            path.lineTo(p3.x, p3.y)
            path.lineTo(p4.x, p4.y)
            canvas.drawPath(path, linePaint)
            
            canvas.drawCircle(p1.x, p1.y, 5f, nodePaint)
            canvas.drawCircle(p3.x, p3.y, 6f, nodePaint)

            // 3. Stylish Callout Box (Bigger)
            val boxWidth = textWidth + padding * 2
            val boxHeight = textHeight + 20
            val boxLeft = if (isRight) p4.x else p4.x - boxWidth
            val boxTop = p4.y - boxHeight / 2
            
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(25, 0, 0, 0)
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, bgPaint)
            
            val accentBarPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = android.graphics.Paint.Style.FILL
            }
            val barX = if (isRight) boxLeft else boxLeft + boxWidth - 6f
            canvas.drawRect(barX, boxTop, barX + 6f, boxTop + boxHeight, accentBarPaint)
            
            canvas.drawLine(boxLeft, boxTop, boxLeft + boxWidth, boxTop, linePaint)
            canvas.drawLine(boxLeft, boxTop + boxHeight, boxLeft + boxWidth, boxTop + boxHeight, linePaint)
            
            canvas.drawText(caption, boxLeft + padding, boxTop + textHeight + 5, paintText)
        }
        
        return BitmapDescriptorFactory.fromBitmap(bitmap)
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
        updateMapMarkers()
        updateFilterIndicators()
    }

    private fun restoreState() {
        if (viewModel.lastSearchCity.isNotEmpty()) {
            etSearchCity.setText(viewModel.lastSearchCity)
            propertyAdapter.updateData(viewModel.searchResults)
            updateMapMarkers()
            tvEmptyState.visibility = if (viewModel.searchResults.isEmpty()) View.VISIBLE else View.GONE
            updatePaginationUI(sessionManager.getPageSize())
            
            if (viewModel.lastClickedPosition != -1) {
                rvSearchResults.scrollToPosition(viewModel.lastClickedPosition)
            }
            
            viewModel.selectedPropertyOnMap?.let {
                showPropertyOnMapCard(it)
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
        updateFilterIndicators()
    }

    private fun updateFilterIndicators() {
        ivFilterCity.visibility = if (etSearchCity.text.isNotEmpty()) View.VISIBLE else View.GONE
        ivFilterMinPrice.visibility = if (etMinPrice.text.isNotEmpty()) View.VISIBLE else View.GONE
        ivFilterMaxPrice.visibility = if (etMaxPrice.text.isNotEmpty()) View.VISIBLE else View.GONE
        ivFilterBedrooms.visibility = if (etBedrooms.text.isNotEmpty()) View.VISIBLE else View.GONE
        ivFilterBathrooms.visibility = if (etBathrooms.text.isNotEmpty()) View.VISIBLE else View.GONE
        ivFilterFloor.visibility = if (etFilterFloor.text.isNotEmpty()) View.VISIBLE else View.GONE
        ivFilterFacing.visibility = if (etFilterFacing.text.isNotEmpty()) View.VISIBLE else View.GONE
        ivFilterRoadSize.visibility = if (etFilterRoadSize.text.isNotEmpty()) View.VISIBLE else View.GONE
        ivFilterProType.visibility = if (etFilterProType.text.isNotEmpty()) View.VISIBLE else View.GONE
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
        // Summary bar is now always visible as requested
        layoutSearchSummary.visibility = View.VISIBLE
        
        layoutFold1.visibility = if (viewModel.isFold1Visible && viewModel.isMainFilterVisible) View.VISIBLE else View.GONE
        layoutFold2.visibility = if (viewModel.isFold2Visible && viewModel.isMainFilterVisible) View.VISIBLE else View.GONE
        
        layoutTopFilters.visibility = if (!viewModel.isFold1Visible && viewModel.isMainFilterVisible) View.VISIBLE else View.GONE
        tvToggleFilters.text = if (viewModel.isFold1Visible) "Hide Filters ▲" else "Show Filters ▼"
        tvToggleFold2.text = if (viewModel.isFold2Visible) "Basic Only ▲" else "Advanced Selection ▼"
        
        tvSearchSummary.text = "Search in ${viewModel.lastSearchCity} (${viewModel.totalCount})"
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
        
        sessionManager.addActivityLog("Property Search", "Searched for properties in $city", "search")
        
        viewModel.lastSearchCity = city
        viewModel.lastClickedPosition = -1
        
        if (viewModel.currentPage == 0) {
            viewModel.searchResults = emptyList()
            propertyAdapter.updateData(emptyList())
            updateMapMarkers()
            viewModel.totalCount = 0
        }
        
        if (!viewModel.isMainFilterVisible) {
            tvSearchSummary.text = "Searching in $city..."
        }
        
        if (viewModel.isMainFilterVisible && viewModel.currentPage == 0) {
            viewModel.isMainFilterVisible = false
            viewModel.isFold1Visible = false
            viewModel.isFold2Visible = false
            updateFoldVisibility()
        }
        
        updateFilterHints()
        updateFilterIndicators()
        searchProgress.visibility = View.VISIBLE

        val pageSize = if (isMapView) 500 else sessionManager.getPageSize()
        val offset = if (isMapView) 0 else viewModel.currentPage * pageSize
        
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getProperties(
                    city = city,
                    categoryId = if (viewModel.selectedProTypeIds.size == 1) viewModel.selectedProTypeIds.first() else null,
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
                
                if (response.status == "success") {
                    val properties = response.data ?: emptyList()
                    viewModel.searchResults = properties
                    viewModel.totalCount = response.count ?: properties.size
                    
                    propertyAdapter.currentCity = viewModel.lastSearchCity
                    propertyAdapter.currentBhk = viewModel.bedrooms
                    propertyAdapter.currentMinPrice = viewModel.minPrice
                    propertyAdapter.currentMaxPrice = viewModel.maxPrice
                    propertyAdapter.currentProTypeIds = viewModel.selectedProTypeIds
                    
                    propertyAdapter.updateData(properties)
                    updateMapMarkers()
                    
                    // Show/Hide views based on results and mode
                    if (properties.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        rvSearchResults.visibility = View.GONE
                        layoutMapContainer.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
                        updateViewModeVisibility() // Ensure correct view is visible
                        if (!isMapView) rvSearchResults.scrollToPosition(0)
                    }
                    
                    updatePaginationUI(pageSize)
                    updateFoldVisibility() 
                    showSwipeHints()
                } else {
                    context?.let { Toast.makeText(it, response.message ?: "Search failed", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                context?.let { Toast.makeText(it, "Error fetching properties", Toast.LENGTH_SHORT).show() }
            } finally {
                searchProgress.visibility = View.GONE
            }
        }
    }

    private fun setupSwipeGestures() {
        val gestureDetector = android.view.GestureDetector(requireContext(), object : android.view.GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 50
            private val SWIPE_VELOCITY_THRESHOLD = 50

            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || e2 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            onSwipeLeft() // Swipe Left -> Next Page
                        } else {
                            onSwipeRight() // Swipe Right -> Previous Page
                        }
                        return true
                    }
                }
                return false
            }
        })

        rvSearchResults.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var isHorizontalSwipe = false

            override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                when (e.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        startX = e.x
                        startY = e.y
                        isHorizontalSwipe = false
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = Math.abs(e.x - startX)
                        val dy = Math.abs(e.y - startY)
                        if (dx > 25 && dx > dy) {
                            isHorizontalSwipe = true
                        }
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        rv.post { isHorizontalSwipe = false }
                    }
                }
                return isHorizontalSwipe
            }
            override fun onTouchEvent(rv: RecyclerView, e: android.view.MotionEvent) {
                gestureDetector.onTouchEvent(e)
            }
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun onSwipeLeft() {
        val pageSize = sessionManager.getPageSize()
        val totalPages = Math.ceil(viewModel.totalCount.toDouble() / pageSize).toInt()
        if (viewModel.currentPage < totalPages - 1) {
            viewModel.currentPage++
            showBottomHint("Navigating to Page ${viewModel.currentPage + 1}")
            updateViewModelFromUI()
            performSearch(viewModel.lastSearchCity)
        }
    }

    private fun onSwipeRight() {
        if (viewModel.currentPage > 0) {
            viewModel.currentPage--
            showBottomHint("Navigating to Page ${viewModel.currentPage + 1}")
            updateViewModelFromUI()
            performSearch(viewModel.lastSearchCity)
        }
    }

    private fun showBottomHint(message: String) {
        if (!sessionManager.isHintsEnabled()) return
        ivHudHintBottom.setImageBitmap(createHudHintBitmap(message, HintPointer.NONE))
        animateHudHint(ivHudHintBottom, isCenter = true)
    }

    private fun showHintAtView(target: View, message: String, pointer: HintPointer) {
        if (!sessionManager.isHintsEnabled()) return
        
        val bitmap = createHudHintBitmap(message, pointer)
        ivHudHintCenter.setImageBitmap(bitmap)
        
        // Ensure measured
        ivHudHintCenter.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        
        val targetLoc = IntArray(2)
        target.getLocationInWindow(targetLoc)
        val rootLoc = IntArray(2)
        view?.getLocationInWindow(rootLoc)
        
        val relativeX = targetLoc[0] - rootLoc[0]
        val relativeY = targetLoc[1] - rootLoc[1]
        
        // Bitmap is 600x300, box is in center.
        // If pointer is UP, anchor is at bottom center of bitmap.
        // If pointer is DOWN, anchor is at top center of bitmap.
        
        val x = relativeX + (target.width / 2) - (bitmap.width / 2)
        val y = if (pointer == HintPointer.UP) {
            relativeY - bitmap.height + 10 // Pointing UP to the view (box is above)
        } else {
            relativeY + target.height - 10 // Pointing DOWN to the view (box is below)
        }
        
        ivHudHintCenter.translationX = x.toFloat()
        ivHudHintCenter.translationY = y.toFloat()
        
        animateHudHint(ivHudHintCenter, isCenter = false, isManualPos = true)
    }

    private fun showHintPopup(message: String) {
        if (!sessionManager.isHintsEnabled()) return
        // Reset translation for center popup
        ivHudHintCenter.translationX = 0f
        ivHudHintCenter.translationY = 0f
        ivHudHintCenter.setImageBitmap(createHudHintBitmap(message, HintPointer.NONE))
        animateHudHint(ivHudHintCenter, isCenter = true)
    }

    private fun createHudHintBitmap(message: String, pointer: HintPointer): Bitmap {
        val width = 600
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val accentColor = android.graphics.Color.parseColor("#E53935")
        val hudLineColor = android.graphics.Color.parseColor("#757575")
        val textColor = android.graphics.Color.parseColor("#424242")
        
        val paintText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            color = textColor
            typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        }
        
        val textWidth = paintText.measureText(message)
        val padding = 20f
        val boxWidth = textWidth + padding * 2
        val boxHeight = 70f
        
        val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = hudLineColor
            strokeWidth = 2.5f
            style = android.graphics.Paint.Style.STROKE
        }
        
        val nodePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = android.graphics.Paint.Style.FILL
        }

        val boxCenterX = width / 2f
        val boxCenterY = height / 2f
        
        val boxLeft = boxCenterX - boxWidth / 2f
        val boxTop = boxCenterY - boxHeight / 2f

        // 1. Draw HUD Box
        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(45, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, bgPaint)
        
        val accentBarPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(boxLeft, boxTop, boxLeft + 6f, boxTop + boxHeight, accentBarPaint)
        canvas.drawRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, linePaint)
        canvas.drawText(message, boxLeft + padding, boxTop + boxHeight / 2 + 10f, paintText)

        // 2. Draw Pointer (Tail)
        if (pointer != HintPointer.NONE) {
            val anchorX = boxCenterX
            val anchorY = if (pointer == HintPointer.UP) boxTop + boxHeight else boxTop
            val targetY = if (pointer == HintPointer.UP) height.toFloat() else 0f
            
            val path = android.graphics.Path()
            path.moveTo(anchorX, anchorY)
            path.lineTo(anchorX, if (pointer == HintPointer.UP) anchorY + 30 else anchorY - 30)
            path.lineTo(anchorX - 40, if (pointer == HintPointer.UP) anchorY + 60 else anchorY - 60)
            path.lineTo(anchorX, targetY)
            canvas.drawPath(path, linePaint)
            
            canvas.drawCircle(anchorX, targetY, 6f, nodePaint)
            canvas.drawCircle(anchorX, anchorY, 4f, nodePaint)
        } else {
            // HUD Decorative Brackets
            val cs = 15f
            canvas.drawLine(boxLeft - 10, boxTop - 10, boxLeft + cs, boxTop - 10, linePaint)
            canvas.drawLine(boxLeft - 10, boxTop - 10, boxLeft - 10, boxTop + cs, linePaint)
            canvas.drawLine(boxLeft + boxWidth + 10, boxTop + boxHeight + 10, boxLeft + boxWidth - cs, boxTop + boxHeight + 10, linePaint)
            canvas.drawLine(boxLeft + boxWidth + 10, boxTop + boxHeight + 10, boxLeft + boxWidth + 10, boxTop + boxHeight - cs, linePaint)
        }
        
        return bitmap
    }

    private fun showSwipeHints() {
        if (isMapView || !sessionManager.isHintsEnabled()) return
        
        val pageSize = sessionManager.getPageSize()
        val totalPages = Math.ceil(viewModel.totalCount.toDouble() / pageSize).toInt()
        if (totalPages <= 1) return

        layoutPageNumbers.post {
            // Next Page Hint
            if (viewModel.currentPage < totalPages - 1) {
                val nextBtn = layoutPageNumbers.getChildAt(viewModel.currentPage + 1)
                if (nextBtn != null) {
                    showHintAtView(nextBtn, "Swipe Left for Page ${viewModel.currentPage + 2} ===>", HintPointer.UP)
                }
            }
            
            // Previous Page Hint
            if (viewModel.currentPage > 0) {
                val prevBtn = layoutPageNumbers.getChildAt(viewModel.currentPage - 1)
                if (prevBtn != null) {
                    // Delay slightly if next hint was also triggered
                    val delay = if (viewModel.currentPage < totalPages - 1) 3000L else 0L
                    layoutPageNumbers.postDelayed({
                        showHintAtView(prevBtn, "<=== Swipe Right for Page ${viewModel.currentPage}", HintPointer.UP)
                    }, delay)
                }
            }
        }
    }

    private fun animateHudHint(view: ImageView, isCenter: Boolean = false, isManualPos: Boolean = false) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        if (!isManualPos) {
            view.translationX = 0f
            view.translationY = if (isCenter) 40f else 0f
        }
        
        view.animate()
            .alpha(1f)
            .translationYBy(if (isManualPos) -20f else -40f)
            .setDuration(450)
            .withEndAction {
                view.animate()
                    .alpha(0f)
                    .translationYBy(100f) // "Fade down" effect
                    .setStartDelay(2000)
                    .setDuration(600)
                    .withEndAction {
                        view.visibility = View.GONE
                    }
                    .start()
            }
            .start()
    }

    private fun updatePaginationUI(pageSize: Int) {
        if (isMapView) {
            layoutPagination.visibility = View.GONE
            return
        }
        val totalPages = Math.ceil(viewModel.totalCount.toDouble() / pageSize).toInt()
        if (totalPages <= 1) {
            layoutPagination.visibility = View.GONE
        } else {
            layoutPagination.visibility = View.VISIBLE
            populatePageNumbers(totalPages)
        }
    }

    private fun populatePageNumbers(totalPages: Int) {
        val ctx = context ?: return
        layoutPageNumbers.removeAllViews()
        val context = ctx
        val density = context.resources.displayMetrics.density
        val size = (30 * density).toInt()
        val margin = (4 * density).toInt()

        for (i in 0 until totalPages) {
            val tv = TextView(context)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, (8 * density).toInt(), margin, 0)
            tv.layoutParams = params
            tv.gravity = android.view.Gravity.CENTER
            tv.text = (i + 1).toString()
            tv.textSize = 12f
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL

            if (i == viewModel.currentPage) {
                drawable.setColor(ContextCompat.getColor(context, R.color.modern_primary))
                tv.setTextColor(android.graphics.Color.WHITE)
            } else {
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
        val explanation = "Tapping an attribute in the list allows you to instantly narrow down results to only properties matching that specific value."
        val spannable = android.text.SpannableString(message + explanation)
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title).setMessage(spannable)
            .setPositiveButton("Apply Only This") { _, _ ->
                viewModel.isMainFilterVisible = false
                clearAllFiltersExcept(type, value)
                viewModel.currentPage = 0
                saveFiltersToPersistence()
                performSearch(viewModel.lastSearchCity)
            }.setNegativeButton("Cancel", null).show()
    }

    private fun clearAllFiltersExcept(type: String, value: Any) {
        val currentCity = viewModel.lastSearchCity
        viewModel.minPrice = null
        viewModel.maxPrice = null
        viewModel.bedrooms = null
        viewModel.bathrooms = null
        viewModel.selectedFloorIds = emptyList()
        viewModel.selectedFacingIds = emptyList()
        viewModel.selectedRoadSizeIds = emptyList()
        viewModel.selectedProTypeIds = emptyList()
        
        etMinPrice.setText(""); etMaxPrice.setText(""); etBedrooms.setText(""); etBathrooms.setText("")
        etFilterFloor.setText(""); etFilterFacing.setText(""); etFilterRoadSize.setText(""); etFilterProType.setText("")

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

    private fun parseAiRequirement(requirement: String) {
        val text = requirement.lowercase()
        viewModel.minPrice = null; viewModel.maxPrice = null; viewModel.bedrooms = null
        val bhkWords = mapOf("one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5)
        var bhkValue: Int? = null
        val bhkRegex = Regex("(\\d+)\\s*bhk")
        bhkRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { bhkValue = it }
        if (bhkValue == null) bhkWords.forEach { (word, value) -> if (text.contains(word)) bhkValue = value }
        bhkValue?.let { viewModel.bedrooms = it; etBedrooms.setText(it.toString()) }

        fun parsePrice(input: String): Double? {
            val clean = input.replace(",", "").replace(" ", "").replace("hazaar", "000").replace("thousand", "000")
            val num = Regex("(\\d+\\.?\\d*)").find(clean)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
            return when {
                clean.contains("cr") || clean.contains("crore") -> num * 1e7
                clean.contains("lakh") || clean.contains("lac") -> num * 1e5
                clean.contains("k") -> num * 1e3
                else -> num
            }
        }
        val budgetTriggers = listOf("under", "below", "within", "budget", "rent", "kiraya", "kiraye", "price", "less than")
        val minPriceTriggers = listOf("above", "more than", "greater than", "start")
        if (budgetTriggers.any { text.contains(it) }) {
            val trigger = budgetTriggers.find { text.contains(it) }!!
            parsePrice(text.substringAfter(trigger))?.let { viewModel.maxPrice = it; etMaxPrice.setText(it.toInt().toString()) }
        } else if (minPriceTriggers.any { text.contains(it) }) {
            val trigger = minPriceTriggers.find { text.contains(it) }!!
            parsePrice(text.substringAfter(trigger))?.let { viewModel.minPrice = it; etMinPrice.setText(it.toInt().toString()) }
        }

        allCities.forEach { city -> if (text.contains(city.lowercase())) { viewModel.lastSearchCity = city; etSearchCity.setText(city) } }
        val categories = CategoryCache.getCategories(requireContext())
        val typeOptions = categories?.find { it.name.contains("Type", true) }?.options ?: emptyList()
        val foundTypeIds = mutableSetOf<Int>(); val foundTypeNames = mutableSetOf<String>()
        val synonyms = mapOf("ghar" to "House", "makan" to "House", "flat" to "Apartment", "apartment" to "Apartment", "villa" to "Villa", "office" to "Office", "shop" to "Shop", "dukan" to "Shop", "plot" to "Plot", "zamin" to "Plot")
        synonyms.forEach { (keyword, targetName) -> if (text.contains(keyword)) { typeOptions.find { it.option.equals(targetName, true) }?.let { foundTypeIds.add(it.categoryId); foundTypeNames.add(it.option) } } }
        typeOptions.forEach { option -> if (text.contains(option.option.lowercase())) { foundTypeIds.add(option.categoryId); foundTypeNames.add(option.option) } }
        if (foundTypeIds.isNotEmpty()) { viewModel.selectedProTypeIds = foundTypeIds.toList(); etFilterProType.setText(foundTypeNames.joinToString(", ")) }

        updateFilterIndicators(); updateFilterHints()
        if (viewModel.lastSearchCity.isNotEmpty()) { viewModel.currentPage = 0; saveFiltersToPersistence(); performSearch(viewModel.lastSearchCity) }
        else { viewModel.isMainFilterVisible = true; updateFoldVisibility(); Toast.makeText(requireContext(), "Filters set! Please specify a city.", Toast.LENGTH_LONG).show() }
    }
}
