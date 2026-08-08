package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.propertyconsultancy.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import android.location.Geocoder
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PropertyAddressFragment : Fragment(), OnMapReadyCallback {

    private lateinit var etAddress1: TextInputEditText
    private lateinit var etAddress2: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etState: TextInputEditText
    private lateinit var etZipCode: TextInputEditText
    private lateinit var tvCoordsDisplay: TextView
    private lateinit var mapOverlay: View
    
    private var googleMap: GoogleMap? = null
    private var currentLat: Double = 21.1458 // Default Nagpur
    private var currentLng: Double = 79.0882
    private var isAutoFilling = false
    private var skipNextGeocode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_property_address, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etAddress1 = view.findViewById(R.id.etAddress1)
        etAddress2 = view.findViewById(R.id.etAddress2)
        etCity = view.findViewById(R.id.etCity)
        etState = view.findViewById(R.id.etState)
        etZipCode = view.findViewById(R.id.etZipCode)
        tvCoordsDisplay = view.findViewById(R.id.tvCoordsDisplay)
        mapOverlay = view.findViewById(R.id.mapOverlay)

        setupTouchInterception()
        setupZipCodeListener()

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapAddress) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun setupTouchInterception() {
        mapOverlay.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    v.performClick()
                }
            }
            false
        }
    }

    private fun setupZipCodeListener() {
        etZipCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isAutoFilling) return
                val zip = s.toString()
                if (zip.length >= 6) {
                    lookupZipCode(zip)
                }
            }
        })
    }

    private fun lookupZipCode(zip: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocationName(zip, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    withContext(Dispatchers.Main) {
                        isAutoFilling = true
                        etCity.setText(addr.locality ?: addr.subAdminArea ?: "")
                        etState.setText(addr.adminArea ?: "")
                        
                        // Also move map to this zip location
                        val latLng = LatLng(addr.latitude, addr.longitude)
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                        isAutoFilling = false
                    }
                }
            } catch (ignore: Exception) {
                isAutoFilling = false
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        
        val initialPos = LatLng(currentLat, currentLng)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPos, 15f))
        
        googleMap?.setOnCameraIdleListener {
            val target = googleMap?.cameraPosition?.target ?: return@setOnCameraIdleListener
            currentLat = target.latitude
            currentLng = target.longitude
            tvCoordsDisplay.text = String.format(Locale.US, "Coordinates: %.6f, %.6f", currentLat, currentLng)
            
            if (!isAutoFilling) {
                if (skipNextGeocode) {
                    skipNextGeocode = false
                } else {
                    reverseGeocode(currentLat, currentLng)
                }
            }
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    withContext(Dispatchers.Main) {
                        isAutoFilling = true
                        
                        // Extract details safely
                        val street = addr.featureName ?: addr.thoroughfare ?: ""
                        val subLocality = addr.subLocality ?: ""
                        
                        etAddress1.setText(street)
                        etAddress2.setText(subLocality)
                        etCity.setText(addr.locality ?: addr.subAdminArea ?: "")
                        etState.setText(addr.adminArea ?: "")
                        etZipCode.setText(addr.postalCode ?: "")
                        isAutoFilling = false
                    }
                }
            } catch (ignored: Exception) {
                isAutoFilling = false
            }
        }
    }

    fun setData(property: com.example.propertyconsultancy.data.dto.PropertyDTO) {
        skipNextGeocode = true // Don't let map move overwrite database values on load
        
        etAddress1.setText(property.addressLine1 ?: "")
        etAddress2.setText(property.addressLine2 ?: "")
        etCity.setText(property.city ?: "")
        etState.setText(property.state ?: "")
        etZipCode.setText(property.zipCode ?: "")
        
        currentLat = property.latitude ?: 21.1458
        currentLng = property.longitude ?: 79.0882
        
        tvCoordsDisplay.text = String.format(Locale.US, "Coordinates: %.6f, %.6f", currentLat, currentLng)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLat, currentLng), 15f))
    }

    fun getData(): Map<String, Any?> {
        return mapOf(
            "address_line_1" to etAddress1.text.toString(),
            "address_line_2" to etAddress2.text.toString(),
            "city" to etCity.text.toString(),
            "state" to etState.text.toString(),
            "zip_code" to etZipCode.text.toString(),
            "latitude" to currentLat,
            "longitude" to currentLng
        )
    }
}
