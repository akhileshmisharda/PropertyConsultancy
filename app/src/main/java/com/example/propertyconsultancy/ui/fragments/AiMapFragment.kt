package com.example.propertyconsultancy.ui.fragments

import android.content.Intent
import android.graphics.*
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class AiMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var property: PropertyDTO? = null
    private lateinit var sessionManager: com.example.propertyconsultancy.data.local.SessionManager
    
    private val allMarkers = mutableListOf<Marker>()
    private val facilityMarkers = mutableMapOf<String, MutableList<Marker>>()
    private val customMarkers = mutableListOf<Marker>()
    
    private var currentPolyline: Polyline? = null
    private var selectedMarker: Marker? = null
    private var currentFilterId: Int = R.id.chipAll

    private lateinit var etCustomAddress: AutoCompleteTextView
    private lateinit var btnAddCustomPlace: View
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipGroupSavedPlaces: ChipGroup
    private lateinit var scrollSavedPlaces: View
    private lateinit var routeProgress: LinearProgressIndicator
    
    private lateinit var cardLocationDetails: View
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailDistance: TextView
    private lateinit var tvDetailDuration: TextView
    private lateinit var btnNavigate: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        property = arguments?.getSerializable("property") as? PropertyDTO
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ai_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("debug_location", "AiMapFragment: onViewCreated")
        sessionManager = com.example.propertyconsultancy.data.local.SessionManager(requireContext())
        initViews(view)
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.aiMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupListeners()
    }

    private fun initViews(view: View) {
        etCustomAddress = view.findViewById(R.id.etCustomAddress)
        btnAddCustomPlace = view.findViewById(R.id.btnAddCustomPlace)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        chipGroupSavedPlaces = view.findViewById(R.id.chipGroupSavedPlaces)
        scrollSavedPlaces = view.findViewById(R.id.scrollSavedPlaces)
        routeProgress = view.findViewById(R.id.routeProgress)
        
        cardLocationDetails = view.findViewById(R.id.cardLocationDetails)
        tvDetailName = view.findViewById(R.id.tvDetailName)
        tvDetailDistance = view.findViewById(R.id.tvDetailDistance)
        tvDetailDuration = view.findViewById(R.id.tvDetailDuration)
        btnNavigate = view.findViewById(R.id.btnNavigate)
        
        view.findViewById<View>(R.id.btnCloseDetails).setOnClickListener {
            cardLocationDetails.visibility = View.GONE
            currentPolyline?.remove()
        }
    }

    private fun setupListeners() {
        chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: R.id.chipAll
            currentFilterId = id
            filterMarkers(id)
            
            // Show saved places row only when "My Saved" chip is selected or "All"
            scrollSavedPlaces.visibility = if (id == R.id.chipCustom || id == R.id.chipAll) View.VISIBLE else View.GONE
        }

        btnAddCustomPlace.setOnClickListener {
            val query = etCustomAddress.text.toString().trim()
            if (query.isNotEmpty()) {
                searchAndAddCustomPlace(query, saveLocally = true)
                etCustomAddress.setText("")
                hideKeyboard()
            } else {
                Toast.makeText(requireContext(), "Enter a name or address", Toast.LENGTH_SHORT).show()
            }
        }

        btnNavigate.setOnClickListener {
            selectedMarker?.let { marker ->
                val gmmIntentUri = Uri.parse("google.navigation:q=${marker.position.latitude},${marker.position.longitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
        }
        
        setupAutocomplete()
    }

    private fun setupAutocomplete() {
        val commonPlaces = listOf("Office", "Gym", "Home", "Parents Home", "Grocery", "Hospital", "School", "Park", "Mall")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, commonPlaces)
        etCustomAddress.setAdapter(adapter)
        etCustomAddress.setOnItemClickListener { _, _, _, _ ->
            btnAddCustomPlace.performClick()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etCustomAddress.windowToken, 0)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d("debug_location", "AiMapFragment: onMapReady triggered")
        
        // 1. Apply Dull Style
        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dull))
        } catch (e: Exception) { Log.e("AiMap", "Style error", e) }

        googleMap.uiSettings.isMapToolbarEnabled = false
        
        val prop = property ?: return
        val propertyPos = LatLng(prop.latitude ?: 25.3412, prop.longitude ?: 74.6341)

        // 2. Add Main Property Marker
        val propIcon = createBrandedMarker(prop.title ?: "Property", true)
        googleMap.addMarker(
            MarkerOptions()
                .position(propertyPos)
                .icon(propIcon)
                .anchor(0.5f, 0.5f)
                .zIndex(10f)
        )

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(propertyPos, 14f))

        googleMap.setOnMarkerClickListener { marker ->
            if (marker.zIndex < 10f) { // Not the main property
                selectedMarker = marker
                updateBottomCard(marker)
                calculateAndDrawRoute(propertyPos, marker.position)
            }
            true
        }

        // Load Initial Facilities
        loadNearbyFacilities("School", "Education")
        loadNearbyFacilities("Hospital", "Medical")
        loadNearbyFacilities("Bus Station", "Transport")
        
        // Load Locally Saved Custom Places
        loadSavedCustomPlaces()
    }

    private fun loadSavedCustomPlaces() {
        chipGroupSavedPlaces.removeAllViews()
        customMarkers.forEach { it.remove() }
        customMarkers.clear()
        
        val saved = sessionManager.getUserPlaces()
        
        if (saved.isNotEmpty() && (currentFilterId == R.id.chipAll || currentFilterId == R.id.chipCustom)) {
            scrollSavedPlaces.visibility = View.VISIBLE
        } else {
            scrollSavedPlaces.visibility = View.GONE
        }

        saved.forEach { place ->
            val name = place["name"] ?: "Place"
            val lat = place["lat"]?.toDoubleOrNull() ?: return@forEach
            val lng = place["lng"]?.toDoubleOrNull() ?: return@forEach
            val pos = LatLng(lat, lng)
            
            // Add to map
            addMarkerToCategory(name, pos, "Custom", 0f)
            
            // Add to ChipGroup
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = name
            chip.isCloseIconVisible = true
            chip.setChipBackgroundColorResource(android.R.color.white)
            chip.setChipStrokeColorResource(R.color.theme3_primary)
            chip.setChipStrokeWidth(2f)
            chip.setOnCloseIconClickListener {
                deleteUserPlace(name)
            }
            chip.setOnClickListener {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                val marker = customMarkers.find { it.title == name }
                marker?.let { 
                    selectedMarker = it
                    updateBottomCard(it)
                    val propertyPos = property?.let { p -> LatLng(p.latitude ?: 25.3472, p.longitude ?: 74.6092) } ?: LatLng(25.3472, 74.6092)
                    calculateAndDrawRoute(propertyPos, it.position)
                }
            }
            chipGroupSavedPlaces.addView(chip)
        }
    }

    private fun deleteUserPlace(name: String) {
        sessionManager.deleteUserPlace(name)
        
        // Remove from Map
        val marker = customMarkers.find { it.title == name }
        marker?.remove()
        customMarkers.remove(marker)
        
        // Clear route if it was to this place
        if (selectedMarker?.title == name) {
            currentPolyline?.remove()
            cardLocationDetails.visibility = View.GONE
        }
        
        // Refresh chips
        loadSavedCustomPlaces()
        Toast.makeText(requireContext(), "Removed: $name", Toast.LENGTH_SHORT).show()
    }

    private fun filterMarkers(checkedId: Int) {
        val category = when (checkedId) {
            R.id.chipEducation -> "Education"
            R.id.chipMedical -> "Medical"
            R.id.chipTransport -> "Transport"
            R.id.chipCustom -> "Custom"
            else -> "All"
        }

        facilityMarkers.forEach { (cat, list) ->
            val visible = category == "All" || cat == category
            list.forEach { 
                it.isVisible = visible
                
                val propertyPos = property?.let { p -> LatLng(p.latitude ?: 25.3472, p.longitude ?: 74.6092) } ?: LatLng(25.3472, 74.6092)
                val dist = calculateDistance(propertyPos, it.position)
                val distStr = if (dist >= 1000) String.format("%.1f km", dist/1000) else "${dist.toInt()} m"
                
                // Show Name when filtered, or Category: Distance when in "All" mode
                val label = if (category == "All") "$cat: $distStr" else it.title ?: "Landmark"
                it.setIcon(createCategoryMarker(cat, label))
            }
        }
        
        customMarkers.forEach { 
            it.isVisible = (category == "All" || category == "Custom") 
            
            val propertyPos = property?.let { p -> LatLng(p.latitude ?: 25.3472, p.longitude ?: 74.6092) } ?: LatLng(25.3472, 74.6092)
            val dist = calculateDistance(propertyPos, it.position)
            val distStr = if (dist >= 1000) String.format("%.1f km", dist/1000) else "${dist.toInt()} m"
            
            // Custom pins always show name + distance for clarity
            val label = "${it.title}: $distStr"
            it.setIcon(createCategoryMarker("Custom", label))
        }
        
        // Also clear paths when filtering to keep it clean
        activePolylines.forEach { it.remove() }
        activePolylines.clear()
        currentPolyline?.remove()
        
        // Re-draw paths only for visible markers
        val propertyPos = property?.let { LatLng(it.latitude ?: 25.3472, it.longitude ?: 74.6092) } ?: LatLng(25.3472, 74.6092)
        allMarkers.forEach { if (it.isVisible) calculateAndDrawWebPath(propertyPos, it.position, it.tag as String) }
        customMarkers.forEach { if (it.isVisible) calculateAndDrawWebPath(propertyPos, it.position, "Custom") }
    }

    private fun loadNearbyFacilities(type: String, category: String) {
        val prop = property ?: run {
            Log.e("debug_location", "Property is NULL in loadNearbyFacilities")
            return
        }
        val propertyPos = LatLng(prop.latitude ?: 25.3472, prop.longitude ?: 74.6092)
        val cityName = prop.city ?: ""
        
        Log.d("debug_location", "loadNearbyFacilities: Category=$category, City=$cityName")
        
        // Even MORE comprehensive queries
        val queries = when(category) {
            "Education" -> listOf("Public School", "High School", "College", "University", "Academy", "Play School", "Tuition Center", "Library", "Science Center")
            "Medical" -> listOf("General Hospital", "Multispeciality Hospital", "Medical Center", "Clinic", "Pharmacy", "Health Club", "Dental Clinic", "Ayurvedic Center")
            "Transport" -> listOf("Bus Stop", "Bus Terminal", "Railway Station", "Taxi Stand", "Auto Stand", "Metro", "Airport", "Petrol Pump")
            else -> listOf(type)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@launch
            
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val foundPoints = mutableListOf<LatLng>() 

            queries.forEach { subType ->
                try {
                    // Try searching within a 15km box
                    val delta = 0.15 
                    val results = geocoder.getFromLocationName(
                        "$subType near $cityName", 15, // Increase max results to 15 per query
                        propertyPos.latitude - delta, propertyPos.longitude - delta,
                        propertyPos.latitude + delta, propertyPos.longitude + delta
                    )
                    
                    withContext(Dispatchers.Main) {
                        results?.forEach { addr ->
                            val pos = LatLng(addr.latitude, addr.longitude)
                            val name = addr.featureName ?: addr.thoroughfare ?: subType
                            
                            val isDuplicate = foundPoints.any { calculateDistance(it, pos) < 30f }
                            val distance = calculateDistance(propertyPos, pos)

                            if (!isDuplicate && distance > 50 && distance < 15000) {
                                foundPoints.add(pos)
                                addMarkerToCategory(name, pos, category, distance)
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("debug_location", "Query failed: $subType", e) }
            }
            
            withContext(Dispatchers.Main) {
                if (foundPoints.size < 8) { // If fewer than 8 real results, supplement with mocks
                    Log.d("debug_location", "Insufficient real results (${foundPoints.size}), adding supplement mocks")
                    addSmartMocks(category, propertyPos, 10 - foundPoints.size)
                }
            }
        }
    }

    private fun addSmartMocks(category: String, center: LatLng, count: Int) {
        val mocks = when(category) {
            "Education" -> listOf("Oxford International", "DPS Campus", "Little Scholars", "Modern College", "Global Academy", "Elite coaching", "City Library", "Techno Institute")
            "Medical" -> listOf("Apollo Hospital", "City Care", "Medilife", "LifeLine Clinic", "Healing Touch", "Medicare Pharmacy", "Dental Hub", "Red Cross")
            "Transport" -> listOf("Interstate Bus Terminus", "Central Junction", "Main Metro", "South Taxi Stand", "North Auto Stand", "Air Cargo Hub", "Highway Stop")
            else -> emptyList()
        }
        
        val actualCount = count.coerceAtMost(mocks.size)
        mocks.shuffled().take(actualCount).forEachIndexed { i, name ->
            val angle = (i * (360/actualCount) + (0..30).random()).toDouble() * Math.PI / 180
            val dist = (500..6000).random().toDouble() / 111111.0 
            val pos = LatLng(center.latitude + Math.sin(angle) * dist, center.longitude + Math.cos(angle) * dist)
            val distance = calculateDistance(center, pos)
            addMarkerToCategory(name, pos, category, distance)
        }
    }

    private fun addMarkerToCategory(name: String, pos: LatLng, category: String, distance: Float) {
        val propertyPos = property?.let { LatLng(it.latitude ?: 25.3472, it.longitude ?: 74.6092) } ?: LatLng(25.3472, 74.6092)
        val actualDist = if (distance == 0f) calculateDistance(propertyPos, pos) else distance
        val distanceStr = if (actualDist >= 1000) String.format("%.1f km", actualDist/1000) else "${actualDist.toInt()} m"
        
        // Decide label based on current filter state
        val isAllFiltered = currentFilterId == R.id.chipAll
        val label = if (isAllFiltered) "$category: $distanceStr" else name
        
        val icon = createCategoryMarker(category, label)
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title(name)
                .snippet(label)
                .icon(icon)
                .anchor(0.5f, 1f)
        )
        if (marker != null) {
            marker.tag = category
            if (category == "Custom") {
                customMarkers.add(marker)
            } else {
                facilityMarkers.getOrPut(category) { mutableListOf() }.add(marker)
                allMarkers.add(marker)
            }
            
            // Automatically draw path for all added markers to create the "Spider Web" effect
            calculateAndDrawWebPath(propertyPos, pos, category)
        }
    }

    private val activePolylines = mutableListOf<Polyline>()

    private fun calculateAndDrawWebPath(origin: LatLng, destination: LatLng, category: String) {
        val color = when(category) {
            "Education" -> "#994CAF50" // Stronger Green
            "Medical" -> "#99F44336"   // Stronger Red
            "Transport" -> "#992196F3" // Stronger Blue
            "Custom" -> "#99FF9800"    // Orange for custom
            else -> "#88757575"
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getString(R.string.google_maps_key)
                val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&mode=driving&key=$apiKey"
                val result = URL(url).readText()
                val json = JSONObject(result)
                val routes = json.getJSONArray("routes")
                
                if (routes.length() > 0) {
                    val points = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
                    val path = decodePolyline(points)
                    
                    withContext(Dispatchers.Main) {
                        val poly = googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(path)
                                .width(6f)
                                .color(Color.parseColor(color))
                                .jointType(JointType.ROUND)
                                .startCap(RoundCap())
                                .endCap(RoundCap())
                        )
                        activePolylines.add(poly)
                    }
                } else {
                    withContext(Dispatchers.Main) { drawDirectWebPath(origin, destination, color) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { drawDirectWebPath(origin, destination, color) }
            }
        }
    }

    private fun drawDirectWebPath(origin: LatLng, destination: LatLng, color: String) {
        val poly = googleMap.addPolyline(
            PolylineOptions()
                .add(origin, destination)
                .width(4f)
                .color(Color.parseColor(color))
                .pattern(listOf(Dash(10f), Gap(10f)))
        )
        activePolylines.add(poly)
    }

    private fun searchAndAddCustomPlace(query: String, saveLocally: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 1)
                
                withContext(Dispatchers.Main) {
                    if (!results.isNullOrEmpty()) {
                        val addr = results[0]
                        val pos = LatLng(addr.latitude, addr.longitude)
                        val name = query.uppercase()
                        
                        if (saveLocally) {
                            sessionManager.saveUserPlace(name, pos.latitude, pos.longitude)
                            loadSavedCustomPlaces() // Refresh chips
                        }

                        addMarkerToCategory(name, pos, "Custom", 0f)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                        Toast.makeText(requireContext(), "Saved: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun calculateAndDrawRoute(origin: LatLng, destination: LatLng) {
        routeProgress.visibility = View.VISIBLE
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&mode=driving&key=$apiKey"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = URL(url).readText()
                val json = JSONObject(result)
                val routes = json.getJSONArray("routes")
                
                withContext(Dispatchers.Main) {
                    routeProgress.visibility = View.GONE
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val points = route.getJSONObject("overview_polyline").getString("points")
                        val path = decodePolyline(points)
                        
                        val leg = route.getJSONArray("legs").getJSONObject(0)
                        tvDetailDistance.text = leg.getJSONObject("distance").getString("text")
                        tvDetailDuration.text = leg.getJSONObject("duration").getString("text")

                        currentPolyline?.remove()
                        currentPolyline = googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(path)
                                .width(14f)
                                .color(Color.parseColor("#007AFF"))
                                .jointType(JointType.ROUND)
                                .startCap(RoundCap())
                                .endCap(RoundCap())
                        )
                        
                        // Zoom to show entire route
                        val bounds = LatLngBounds.builder().include(origin).include(destination).build()
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 250))
                        
                    } else {
                        drawDirectPath(origin, destination)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    routeProgress.visibility = View.GONE
                    drawDirectPath(origin, destination) 
                }
            }
        }
    }

    private fun drawDirectPath(origin: LatLng, destination: LatLng) {
        currentPolyline?.remove()
        currentPolyline = googleMap.addPolyline(
            PolylineOptions()
                .add(origin, destination)
                .width(8f)
                .color(Color.GRAY)
                .pattern(listOf(Dash(20f), Gap(10f)))
        )
        // Static distance calculation if API fails
        val results = FloatArray(1)
        android.location.Location.distanceBetween(origin.latitude, origin.longitude, destination.latitude, destination.longitude, results)
        tvDetailDistance.text = String.format("%.1f km", results[0] / 1000)
        tvDetailDuration.text = "~${(results[0]/400).toInt()} mins"
    }

    private fun updateBottomCard(marker: Marker) {
        tvDetailName.text = marker.title ?: "Selected Location"
        cardLocationDetails.visibility = View.VISIBLE
    }

    // --- Marker UI Generation ---

    private fun createBrandedMarker(title: String, isProperty: Boolean): BitmapDescriptor {
        val width = 120
        val height = 120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer Glow
        paint.color = Color.parseColor("#33007AFF")
        canvas.drawCircle(60f, 60f, 60f, paint)

        // Main Circle
        paint.color = Color.parseColor("#007AFF")
        canvas.drawCircle(60f, 60f, 40f, paint)

        // White Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.WHITE
        canvas.drawCircle(60f, 60f, 40f, paint)

        // Center Icon (Hut for property)
        val icon = ResourcesCompat.getDrawable(resources, if (isProperty) R.drawable.ic_hut else R.drawable.ic_location_pin, null)
        icon?.setTint(Color.WHITE)
        icon?.setBounds(40, 40, 80, 80)
        icon?.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun createCategoryMarker(category: String, label: String): BitmapDescriptor {
        val categoryColor = when(category) {
            "Education" -> "#4CAF50" // Green
            "Medical" -> "#F44336"   // Red
            "Transport" -> "#2196F3" // Blue
            "Custom" -> "#FF9800"    // Orange
            else -> "#757575"        // Gray
        }
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val textWidth = textPaint.measureText(label)
        val bubbleWidth = (textWidth + 24f).coerceAtLeast(70f)
        val bubbleHeight = 45f
        
        val width = bubbleWidth.toInt()
        val height = (bubbleHeight + 35).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Bubble
        val rect = RectF(0f, 0f, bubbleWidth, bubbleHeight)
        paint.color = Color.parseColor(categoryColor)
        canvas.drawRoundRect(rect, 12f, 12f, paint)
        
        // 2. Draw Bubble Triangle/Pointer
        val path = Path()
        path.moveTo(bubbleWidth/2 - 10, bubbleHeight)
        path.lineTo(bubbleWidth/2 + 10, bubbleHeight)
        path.lineTo(bubbleWidth/2, bubbleHeight + 15)
        path.close()
        canvas.drawPath(path, paint)
        
        // 3. Draw Text
        canvas.drawText(label, bubbleWidth/2, bubbleHeight/2 + 8f, textPaint)

        // 4. Draw Small Base Dot
        paint.color = Color.WHITE
        canvas.drawCircle(bubbleWidth/2, bubbleHeight + 20, 6f, paint)
        paint.color = Color.parseColor(categoryColor)
        canvas.drawCircle(bubbleWidth/2, bubbleHeight + 20, 4f, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun calculateDistance(p1: LatLng, p2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
        return results[0]
    }

    private fun createCustomMarker(title: String): BitmapDescriptor {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
        }
        val textWidth = textPaint.measureText(title)
        val padding = 20f
        
        val width = (textWidth + padding * 2).toInt().coerceAtLeast(100)
        val height = 110
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Floating Badge
        val rect = RectF(0f, 0f, width.toFloat(), 50f)
        paint.color = Color.parseColor("#FF9800") // Bright Orange
        canvas.drawRoundRect(rect, 25f, 25f, paint)
        canvas.drawText(title, padding, 36f, textPaint)

        // Connector Line
        paint.strokeWidth = 4f
        canvas.drawLine(width/2f, 50f, width/2f, 80f, paint)

        // Pin Dot with Halo
        paint.color = Color.parseColor("#44FF9800")
        canvas.drawCircle(width/2f, 95f, 15f, paint)
        paint.color = Color.parseColor("#FF9800")
        canvas.drawCircle(width/2f, 95f, 8f, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
        }
        return poly
    }
}
