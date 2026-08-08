package com.example.propertyconsultancy.ui.fragments

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.cache.CategoryCache
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.example.propertyconsultancy.utils.FileUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.transition.TransitionInflater
import androidx.lifecycle.ViewModelProvider
import com.example.propertyconsultancy.ui.viewmodels.SearchViewModel

class AddPropertyFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: PropertyPagerAdapter

    private lateinit var layoutProgress: View
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvProgressPercent: TextView
    private lateinit var btnSubmit: Button
    private lateinit var sessionManager: SessionManager
    private lateinit var searchViewModel: SearchViewModel
    private var propertyToEdit: PropertyDTO? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_add_property, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        searchViewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        propertyToEdit = arguments?.getSerializable("property") as? PropertyDTO

        val logAction = if (propertyToEdit != null) "Editing property: ${propertyToEdit?.title}" else "Adding new property"
        sessionManager.addActivityLog("Property Management", logAction, "add")

        (activity as? MainActivity)?.let {
            it.updateTitle(if (propertyToEdit != null) "Edit Property" else "Property Addition")
            it.setBottomNavVisibility(false)
        }

        layoutProgress = view.findViewById(R.id.layoutProgress)
        progressBar = view.findViewById(R.id.progressBar)
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent)
        btnSubmit = view.findViewById(R.id.btnSubmit)

        setupViewPager(view)
        fetchCategoriesBackground()

        if (propertyToEdit != null) {
            btnSubmit.text = "Update Property"
        }
        btnSubmit.setOnClickListener {
            if (pagerAdapter.pricingFragment.validate()) {
                submitProperty()
            }
        }

        if (propertyToEdit != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                propertyToEdit?.let { 
                    Log.d("[php_debug]", "AddPropertyFragment initializing fragments with property ID: ${it.propertyId}")
                    Log.d("[php_debug]", "Full Address Data from Server: Addr1=${it.addressLine1}, Addr2=${it.addressLine2}, City=${it.city}, State=${it.state}, Zip=${it.zipCode}, Lat=${it.latitude}, Lng=${it.longitude}")
                    
                    pagerAdapter.detailsFragment.setData(it)
                    pagerAdapter.amenitiesFragment.setData(it)
                    pagerAdapter.pricingFragment.setData(it)
                    pagerAdapter.addressFragment.setData(it)
                    pagerAdapter.mediaFragment.setData(it.propertyId, it.media)
                }
            }, 500)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.setBottomNavVisibility(true)
    }

    private fun setupViewPager(view: View) {
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        pagerAdapter = PropertyPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 5
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnSubmit.visibility = if (position == 4) View.GONE else View.VISIBLE
                if (position == 4 && propertyToEdit?.propertyId == null) {
                    // Try to save property first to get an ID for direct media upload
                    Toast.makeText(requireContext(), "Initializing property for media upload...", Toast.LENGTH_SHORT).show()
                    savePropertySilent()
                }
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = when (pos) { 
                0 -> "Details"
                1 -> "Pricing"
                2 -> "Address"
                3 -> "Amenities"
                else -> "Media"
            }
        }.attach()
    }

    private fun savePropertySilent() {
        // Collect data from other fragments
        val details = pagerAdapter.detailsFragment.getData()
        val pricing = pagerAdapter.pricingFragment.getData()
        val address = pagerAdapter.addressFragment.getData()
        val amenities = pagerAdapter.amenitiesFragment.getData()
        val user = sessionManager.getUser()

        val payload = mutableMapOf<String, Any?>()
        payload["landlord_id"] = user?.userId
        payload["title"] = details["title"] ?: "Untitled Property"
        payload["description"] = details["description"]
        payload["bedrooms"] = (pricing["rooms"] as? String)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        payload["bathrooms"] = pricing["bathrooms"]
        payload["area_sqft"] = pricing["area"]
        payload["furnishing"] = pricing["furnishing"]
        payload["status"] = "draft"
        payload["price_per_month"] = (pricing["price"] as? String)?.toDoubleOrNull() ?: 0.0
        payload["latitude"] = address["latitude"]
        payload["longitude"] = address["longitude"]
        payload["address_line_1"] = address["address_line_1"]
        payload["address_line_2"] = address["address_line_2"]
        payload["city"] = address["city"]
        payload["state"] = address["state"]
        payload["zip_code"] = address["zip_code"]
        payload["amenity_ids"] = amenities["amenity_ids"]
        
        details.forEach { (k, v) -> if (k !in listOf("title", "description")) payload[k] = v }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = RetrofitInstance.api.submitProperty(payload)
                if (resp.status == "success" && resp.propertyId != null) {
                    withContext(Dispatchers.Main) {
                        propertyToEdit = PropertyDTO(propertyId = resp.propertyId)
                        pagerAdapter.mediaFragment.setData(resp.propertyId, null)
                        Log.d("AddProperty", "Property initialized with ID: ${resp.propertyId}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error initializing property: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchCategoriesBackground() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getCategories()
                if (response.status == "success") CategoryCache.saveCategories(requireContext(), response.data)
            } catch (e: Exception) {}
        }
    }

    private fun submitProperty() {
        layoutProgress.visibility = View.VISIBLE; tvProgressPercent.text = "0%"
        val details = pagerAdapter.detailsFragment.getData()
        val pricing = pagerAdapter.pricingFragment.getData()
        val address = pagerAdapter.addressFragment.getData()
        val amenities = pagerAdapter.amenitiesFragment.getData()
        val mediaImages = pagerAdapter.mediaFragment.getSelectedImages()
        val mediaVideos = pagerAdapter.mediaFragment.getSelectedVideos()
        val user = sessionManager.getUser()

        CoroutineScope(Dispatchers.IO).launch {
            val processedMedia = (mediaImages + mediaVideos).mapNotNull { uri ->
                if (uri.scheme?.startsWith("http") == true) {
                    val url = uri.toString()
                    // Strip the domain and everything after '?' (cache busters)
                    url.substringAfter("property/").substringBefore("?")
                } else {
                    val b64 = FileUtils.encodeUriToBase64(requireContext(), uri)
                    if (b64 != null) {
                        (if (mediaVideos.contains(uri)) "data:video/mp4;base64," else "data:image/jpeg;base64,") + b64
                    } else null
                }
            }
            
            val payload = mutableMapOf<String, Any?>()
            payload["property_id"] = propertyToEdit?.propertyId
            payload["landlord_id"] = user?.userId
            payload["title"] = details["title"]
            payload["description"] = details["description"]
            payload["bedrooms"] = (pricing["rooms"] as? String)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            payload["bathrooms"] = pricing["bathrooms"]
            payload["area_sqft"] = pricing["area"]
            payload["furnishing"] = pricing["furnishing"]
            payload["status"] = "available"
            payload["price_per_month"] = (pricing["price"] as? String)?.toDoubleOrNull() ?: 0.0
            payload["latitude"] = address["latitude"]
            payload["longitude"] = address["longitude"]
            payload["address_line_1"] = address["address_line_1"]
            payload["address_line_2"] = address["address_line_2"]
            payload["city"] = address["city"]
            payload["state"] = address["state"]
            payload["zip_code"] = address["zip_code"]
            payload["country"] = "India"
            payload["amenity_ids"] = amenities["amenity_ids"]
            payload["media_urls"] = processedMedia
            payload["floor_id"] = details["floor_id"]
            payload["facing_id"] = details["facing_id"]
            payload["roadsize_id"] = details["roadsize_id"]
            payload["protype_id"] = details["protype_id"]
            payload["status_id"] = details["status_id"]
            
            // Merge all dynamic fields from details fragment
            details.forEach { (k, v) ->
                if (k !in listOf("title", "description", "floor_id", "facing_id", "roadsize_id", "protype_id", "status_id")) {
                    payload[k] = v
                }
            }
            
            try {
                val resp = if (payload["property_id"] != null) RetrofitInstance.api.updateProperty(payload) else RetrofitInstance.api.submitProperty(payload)
                withContext(Dispatchers.Main) {
                    if (resp.status == "success") { 
                        searchViewModel.shouldRefresh = true
                        Toast.makeText(requireContext(), "Property saved successfully!", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressed() 
                    }
                    else { layoutProgress.visibility = View.GONE; Toast.makeText(requireContext(), resp.message, Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) { 
                withContext(Dispatchers.Main) { 
                    layoutProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } 
            }
        }
    }

    private class PropertyPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        val detailsFragment = PropertyDetailsFragment()
        val pricingFragment = PropertyPricingFragment()
        val addressFragment = PropertyAddressFragment()
        val amenitiesFragment = PropertyAmenitiesFragment()
        val mediaFragment = PropertyMediaFragment()
        
        override fun getItemCount(): Int = 5
        override fun createFragment(pos: Int): Fragment = when (pos) { 
            0 -> detailsFragment
            1 -> pricingFragment
            2 -> addressFragment
            3 -> amenitiesFragment
            else -> mediaFragment
        }
    }
}
