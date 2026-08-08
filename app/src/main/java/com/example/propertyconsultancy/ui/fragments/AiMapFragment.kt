package com.example.propertyconsultancy.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AiMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var property: PropertyDTO? = null
    private val facilityMarkers = mutableListOf<Marker>()
    private var currentPolyline: Polyline? = null

    private lateinit var tvFacilityName: TextView
    private lateinit var tvDistanceText: TextView
    private lateinit var cardDistanceDetail: View

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
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.aiMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

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

        mockData.forEach { (name, pos) ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(name)
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
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title(name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        )
        if (marker != null) {
            facilityMarkers.add(marker)
            val propertyPos = property?.let { LatLng(it.latitude ?: 0.0, it.longitude ?: 0.0) } ?: LatLng(25.3412, 74.6341)
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
