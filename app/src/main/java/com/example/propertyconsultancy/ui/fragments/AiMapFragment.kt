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
    
    private val allMarkers = mutableListOf<Marker>()
    private val facilityMarkers = mutableMapOf<String, MutableList<Marker>>()
    private val customMarkers = mutableListOf<Marker>()
    
    private var currentPolyline: Polyline? = null
    private var selectedMarker: Marker? = null

    private lateinit var etCustomAddress: AutoCompleteTextView
    private lateinit var btnAddCustomPlace: View
    private lateinit var chipGroupCategories: ChipGroup
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
        initViews(view)
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.aiMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupListeners()
    }

    private fun initViews(view: View) {
        etCustomAddress = view.findViewById(R.id.etCustomAddress)
        btnAddCustomPlace = view.findViewById(R.id.btnAddCustomPlace)
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
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
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            filterMarkers(id)
        }

        btnAddCustomPlace.setOnClickListener {
            val query = etCustomAddress.text.toString().trim()
            if (query.isNotEmpty()) {
                searchAndAddCustomPlace(query)
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
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
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
            list.forEach { it.isVisible = visible }
        }
        
        customMarkers.forEach { it.isVisible = (category == "All" || category == "Custom") }
    }

    private fun loadNearbyFacilities(type: String, category: String) {
        val prop = property ?: run {
            Log.e("debug_location", "Property is NULL in loadNearbyFacilities")
            return
        }
        val propertyPos = LatLng(prop.latitude ?: 25.3412, prop.longitude ?: 74.6341)
        val cityName = prop.city ?: ""
        
        Log.d("debug_location", "loadNearbyFacilities: Category=$category, City=$cityName, Pos=$propertyPos")
        val queries = when(category) {
            "Education" -> listOf("School near $cityName", "College near $cityName", "University near $cityName", "Coaching Center near $cityName")
            "Medical" -> listOf("Hospital near $cityName", "Clinic near $cityName", "Pharmacy near $cityName", "Diagnostic Center near $cityName")
            "Transport" -> listOf("Bus Station near $cityName", "Railway Station near $cityName", "Metro Station near $cityName", "Taxi Stand near $cityName")
            else -> listOf("$type near $cityName")
        }

        lifecycleScope.launch(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                Log.e("debug_location", "Geocoder is NOT present on this device!")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Location search not supported on this device", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            Log.d("debug_location", "Starting search for $category in city: $cityName")
            val foundLocations = mutableSetOf<String>() // To avoid duplicates

            queries.forEach { query ->
                try {
                    Log.d("debug_location", "Running Geocoder query: $query")
                    
                    // Create a bounding box (~20km) around the property to improve local search accuracy
                    val latDelta = 0.15 // roughly 15-20km
                    val lngDelta = 0.15
                    
                    var results = geocoder.getFromLocationName(
                        query, 15,
                        propertyPos.latitude - latDelta, propertyPos.longitude - lngDelta,
                        propertyPos.latitude + latDelta, propertyPos.longitude + lngDelta
                    )
                    
                    // Fallback: If no results with "near City", try just the type (School, etc) within bounds
                    if (results.isNullOrEmpty()) {
                        val simpleQuery = query.substringBefore(" near")
                        Log.d("debug_location", "Retrying with simple query: $simpleQuery")
                        results = geocoder.getFromLocationName(
                            simpleQuery, 15,
                            propertyPos.latitude - latDelta, propertyPos.longitude - lngDelta,
                            propertyPos.latitude + latDelta, propertyPos.longitude + lngDelta
                        )
                    }
                    
                    Log.d("debug_location", "Query: $query -> Found ${results?.size ?: 0} results")
                    
                    withContext(Dispatchers.Main) {
                        results?.forEach { addr ->
                            val pos = LatLng(addr.latitude, addr.longitude)
                            val name = addr.featureName ?: addr.thoroughfare ?: type
                            val distance = calculateDistance(propertyPos, pos)
                            
                            Log.d("debug_location", "Candidate: $name at $pos. Distance: ${distance/1000}km")
                            
                            // Only show within 15km for "Nearby" relevance
                            if (distance < 15000 && !foundLocations.contains(name)) {
                                foundLocations.add(name)
                                addMarkerToCategory(name, pos, category, distance)
                                Log.d("debug_location", "Added Pin: $name ($category)")
                            } else {
                                if (distance >= 15000) Log.d("debug_location", "Skipped (Too far): $name")
                                if (foundLocations.contains(name)) Log.d("debug_location", "Skipped (Duplicate): $name")
                            }
                        }
                    }
                } catch (e: Exception) { 
                    Log.e("debug_location", "Query failed: $query", e) 
                }
            }
        }
    }

    private fun addMarkerToCategory(name: String, pos: LatLng, category: String, distance: Float) {
        val distanceStr = if (distance >= 1000) String.format("%.1f km", distance/1000) else "${distance.toInt()} m"
        val icon = createCategoryMarker(category, distanceStr)
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title(name)
                .snippet("Distance: $distanceStr")
                .icon(icon)
                .anchor(0.5f, 1f) // Anchor bottom center for the pin style
        )
        if (marker != null) {
            marker.tag = category
            facilityMarkers.getOrPut(category) { mutableListOf() }.add(marker)
            allMarkers.add(marker)
        }
    }

    private fun searchAndAddCustomPlace(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 1)
                
                withContext(Dispatchers.Main) {
                    if (!results.isNullOrEmpty()) {
                        val addr = results[0]
                        val pos = LatLng(addr.latitude, addr.longitude)
                        val name = query.uppercase()
                        
                        val icon = createCustomMarker(name)
                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(pos)
                                .icon(icon)
                                .anchor(0.5f, 0.5f)
                                .zIndex(5f)
                        )
                        if (marker != null) {
                            marker.title = name
                            customMarkers.add(marker)
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                            Toast.makeText(requireContext(), "Saved: $name", Toast.LENGTH_SHORT).show()
                        }
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

    private fun createCategoryMarker(category: String, distance: String): BitmapDescriptor {
        val categoryColor = when(category) {
            "Education" -> "#4CAF50" // Green
            "Medical" -> "#F44336"   // Red
            "Transport" -> "#2196F3" // Blue
            else -> "#757575"        // Gray
        }
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val textWidth = textPaint.measureText(distance)
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
        
        // 3. Draw Distance Text
        canvas.drawText(distance, bubbleWidth/2, bubbleHeight/2 + 8f, textPaint)

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
