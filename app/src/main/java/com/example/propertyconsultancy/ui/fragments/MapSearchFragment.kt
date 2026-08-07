package com.example.propertyconsultancy.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch
import java.util.Locale

class MapSearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var tvRadiusLabel: TextView
    private lateinit var radiusSlider: Slider
    
    private var searchCircle: Circle? = null
    private var currentCenter = LatLng(25.3412, 74.6341) // Bhilwara default
    private var currentRadiusKm = 2.0
    private var cachedProperties: List<PropertyDTO> = emptyList()
    private val markers = mutableListOf<Marker>()

    companion object {
        private const val ARG_PROPERTIES = "properties"

        fun newInstance(properties: List<PropertyDTO>): MapSearchFragment {
            val fragment = MapSearchFragment()
            val args = Bundle()
            args.putSerializable(ARG_PROPERTIES, ArrayList(properties))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvRadiusLabel = view.findViewById(R.id.tvRadiusLabel)
        radiusSlider = view.findViewById(R.id.radiusSlider)
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        radiusSlider.addOnChangeListener { _, value, _ ->
            currentRadiusKm = value.toDouble()
            tvRadiusLabel.text = String.format(Locale.getDefault(), "Search Radius: %.1f km", currentRadiusKm)
            updateSearchCircle()
            dropPins(cachedProperties)
        }

        view.findViewById<View>(R.id.fabCurrentLocation).setOnClickListener {
            if (checkLocationPermission()) {
                googleMap.isMyLocationEnabled = true
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCenter, 12f))
            }
        }

        // Load properties from arguments if available
        val passedProperties = arguments?.getSerializable(ARG_PROPERTIES) as? ArrayList<PropertyDTO>
        if (passedProperties != null) {
            cachedProperties = passedProperties
            // If we have properties, we might want to center on the first one or stay on default
            if (cachedProperties.isNotEmpty()) {
                val first = cachedProperties.first()
                if (first.latitude != null && first.longitude != null) {
                    currentCenter = LatLng(first.latitude, first.longitude)
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        
        if (checkLocationPermission()) {
            googleMap.isMyLocationEnabled = true
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentCenter, 12f))
        
        drawSearchCircle()
        
        if (cachedProperties.isEmpty()) {
            fetchPropertiesAndDropPins()
        } else {
            dropPins(cachedProperties)
        }

        googleMap.setOnMapClickListener { latLng ->
            currentCenter = latLng
            updateSearchCircle()
            dropPins(cachedProperties)
        }
    }

    private fun drawSearchCircle() {
        searchCircle = googleMap.addCircle(
            CircleOptions()
                .center(currentCenter)
                .radius(currentRadiusKm * 1000) // Meters
                .strokeWidth(2f)
                .strokeColor(Color.BLUE)
                .fillColor(Color.argb(30, 0, 0, 255))
        )
    }

    private fun updateSearchCircle() {
        searchCircle?.center = currentCenter
        searchCircle?.radius = currentRadiusKm * 1000
    }

    private fun fetchPropertiesAndDropPins() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getProperties(city = "Bhilwara", limit = 100)
                if (response.status == "success") {
                    cachedProperties = response.data ?: emptyList()
                    dropPins(cachedProperties)
                }
            } catch (e: Exception) { }
        }
    }

    private fun dropPins(properties: List<PropertyDTO>) {
        googleMap.clear()
        drawSearchCircle() 
        markers.clear()

        properties.forEach { property ->
            val lat = property.latitude
            val lng = property.longitude
            
            if (lat != null && lng != null) {
                val pos = LatLng(lat, lng)
                val distance = calculateDistance(currentCenter, pos)
                
                if (distance <= (currentRadiusKm * 1000)) {
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title(property.title)
                            .snippet("₹${property.pricePerMonth?.toInt()} | ${property.bedrooms} BHK")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    if (marker != null) {
                        marker.tag = property
                        markers.add(marker)
                    }
                }
            }
        }

        googleMap.setOnInfoWindowClickListener { marker ->
            val property = marker.tag as? PropertyDTO
            if (property != null) {
                (activity as? MainActivity)?.openPropertyExplore(property)
            }
        }
    }

    private fun calculateDistance(p1: LatLng, p2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
        return results[0]
    }

    private fun checkLocationPermission() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
