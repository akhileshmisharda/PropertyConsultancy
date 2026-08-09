package com.example.propertyconsultancy.ui.fragments

import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class AiMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var property: PropertyDTO? = null
    private val facilityMarkers = mutableListOf<Marker>()
    private var currentPolyline: Polyline? = null

    private lateinit var tvFacilityName: TextView
    private lateinit var tvDistanceText: TextView
    private lateinit var cardDistanceDetail: View
    private lateinit var btnShowPath: View
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        property = arguments?.getSerializable("property") as? PropertyDTO
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ai_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvFacilityName = view.findViewById(R.id.tvFacilityName)
        tvDistanceText = view.findViewById(R.id.tvDistanceText)
        cardDistanceDetail = view.findViewById(R.id.cardDistanceDetail)
        btnShowPath = view.findViewById(R.id.btnShowPath)
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.aiMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnShowPath.setOnClickListener {
            val propertyPos = property?.let { LatLng(it.latitude ?: 0.0, it.longitude ?: 0.0) } ?: LatLng(25.3412, 74.6341)
            selectedMarker?.let { marker ->
                drawRealPath(propertyPos, marker.position)
            }
        }

        view.findViewById<ChipGroup>(R.id.chipGroupFacilities).setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            when (checkedId) {
                R.id.chipSchool -> showFacilities("School")
                R.id.chipHospital -> showFacilities("Hospital")
                R.id.chipTransit -> showFacilities("Transit")
                R.id.chipUserLoc -> showFacilities("Personal")
            }
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddUserPlace).setOnClickListener {
            showAddPlaceDialog()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        
        val lat = property?.latitude ?: 25.3412
        val lng = property?.longitude ?: 74.6341
        val propertyPos = LatLng(lat, lng)

        // Mark Property
        googleMap.addMarker(
            MarkerOptions()
                .position(propertyPos)
                .title(property?.title ?: "Property Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(propertyPos, 14f))

        googleMap.setOnMarkerClickListener { marker ->
            if (marker.title != property?.title) {
                selectedMarker = marker
                showDistanceInfo(propertyPos, marker)
            }
            false
        }
    }

    private fun showFacilities(type: String) {
        facilityMarkers.forEach { it.remove() }
        facilityMarkers.clear()
        currentPolyline?.remove()
        cardDistanceDetail.visibility = View.GONE

        val prop = property ?: return
        if (type == "Personal") {
            // Keep existing personal markers if any, or just clear non-personal
            // For now, just a Toast as 'My Places' are added manually via FAB
            Toast.makeText(requireContext(), "Use '+' button to add your custom places", Toast.LENGTH_SHORT).show()
            return
        }
        val cityName = prop.city ?: "City"
        val query = "$type near $cityName"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 10)
                
                withContext(Dispatchers.Main) {
                    if (results.isNullOrEmpty()) {
                        // Fallback to mock if geocoder fails or returns nothing
                        addMockFacilities(type)
                    } else {
                        results.forEach { addr ->
                            val pos = LatLng(addr.latitude, addr.longitude)
                            val name = addr.featureName ?: addr.thoroughfare ?: type
                            addFacilityMarker(name, pos, type)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { addMockFacilities(type) }
            }
        }
    }

    private fun addMockFacilities(type: String) {
        val lat = property?.latitude ?: 25.3412
        val lng = property?.longitude ?: 74.6341
        val mockData = when (type) {
            "School" -> listOf(
                Pair("City International School", LatLng(lat + 0.005, lng + 0.003)),
                Pair("Greenwood High", LatLng(lat - 0.008, lng + 0.006))
            )
            "Hospital" -> listOf(
                Pair("LifeCare Hospital", LatLng(lat + 0.012, lng - 0.004)),
                Pair("Metro Clinic", LatLng(lat - 0.003, lng - 0.007))
            )
            "Transit" -> listOf(
                Pair("Main Bus Station", LatLng(lat + 0.015, lng + 0.010)),
                Pair("Railway Junction", LatLng(lat - 0.020, lng - 0.015))
            )
            else -> emptyList()
        }
        mockData.forEach { (name, pos) -> addFacilityMarker(name, pos, type) }
    }

    private fun addFacilityMarker(name: String, pos: LatLng, type: String) {
        val propertyPos = property?.let { LatLng(it.latitude ?: 0.0, it.longitude ?: 0.0) } ?: LatLng(25.3412, 74.6341)
        val distance = calculateDistance(propertyPos, pos)
        val distanceStr = String.format("%.2f km", distance / 1000)

        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title("$name ($distanceStr)")
                .snippet("Click for Real Path")
                .icon(BitmapDescriptorFactory.defaultMarker(when(type) {
                    "School" -> BitmapDescriptorFactory.HUE_AZURE
                    "Hospital" -> BitmapDescriptorFactory.HUE_ORANGE
                    "Transit" -> BitmapDescriptorFactory.HUE_VIOLET
                    else -> BitmapDescriptorFactory.HUE_YELLOW
                }))
        )
        if (marker != null) {
            marker.tag = type
            facilityMarkers.add(marker)
        }
    }

    private fun drawRealPath(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&mode=driving&key=$apiKey"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = URL(url).readText()
                val json = JSONObject(result)
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val points = route.getJSONObject("overview_polyline").getString("points")
                    val path = decodePolyline(points)
                    
                    val legs = route.getJSONArray("legs")
                    val distanceText = if (legs.length() > 0) legs.getJSONObject(0).getJSONObject("distance").getString("text") else ""
                    
                    withContext(Dispatchers.Main) {
                        currentPolyline?.remove()
                        currentPolyline = googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(path)
                                .width(12f)
                                .color(Color.parseColor("#007AFF"))
                                .jointType(JointType.ROUND)
                                .startCap(RoundCap())
                                .endCap(RoundCap())
                        )
                        if (distanceText.isNotEmpty()) {
                            tvDistanceText.text = "Real Distance: $distanceText (Driving)"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No real path found, showing direct line", Toast.LENGTH_SHORT).show()
                        drawDirectPath(origin, destination)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { drawDirectPath(origin, destination) }
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
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    private fun showAddPlaceDialog() {
        val context = requireContext()
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle("Add Important Location")
        
        val input = android.widget.EditText(context)
        input.hint = "e.g. Office, Kids School"
        builder.setView(input)

        builder.setPositiveButton("Add Current Center") { _, _ ->
            val name = input.text.toString()
            if (name.isNotEmpty()) {
                val center = googleMap.cameraPosition.target
                addUserPlace(name, center)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun addUserPlace(name: String, pos: LatLng) {
        val propertyPos = property?.let { LatLng(it.latitude ?: 0.0, it.longitude ?: 0.0) } ?: LatLng(25.3412, 74.6341)
        val distance = calculateDistance(propertyPos, pos)
        val distanceStr = String.format("%.2f km", distance / 1000)

        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title("$name ($distanceStr)")
                .snippet("Click for Real Path")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        )
        if (marker != null) {
            facilityMarkers.add(marker)
            selectedMarker = marker
            showDistanceInfo(propertyPos, marker)
        }
    }

    private fun showDistanceInfo(propertyPos: LatLng, facilityMarker: Marker) {
        val facilityPos = facilityMarker.position
        val distance = calculateDistance(propertyPos, facilityPos)
        
        tvFacilityName.text = facilityMarker.title
        tvDistanceText.text = String.format("%.2f km away from property", distance / 1000)
        cardDistanceDetail.visibility = View.VISIBLE

        currentPolyline?.remove()
        currentPolyline = googleMap.addPolyline(
            PolylineOptions()
                .add(propertyPos, facilityPos)
                .width(8f)
                .color(Color.BLUE)
                .pattern(listOf(Dash(20f), Gap(10f)))
                .geodesic(true)
        )
    }

    private fun calculateDistance(p1: LatLng, p2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
        return results[0]
    }
}
