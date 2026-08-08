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
    private lateinit var sessionManager: SessionManager
    private var propertyToEdit: PropertyDTO? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_add_property, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
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

        setupViewPager(view)
        fetchCategoriesBackground()

        view.findViewById<View>(R.id.btnSubmit).setOnClickListener {
            if (pagerAdapter.pricingFragment.validate()) {
                submitProperty()
            }
        }

        if (propertyToEdit != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                propertyToEdit?.let { 
                    pagerAdapter.detailsFragment.setData(it)
                    pagerAdapter.amenitiesFragment.setData(it)
                    pagerAdapter.pricingFragment.setData(it)
                    pagerAdapter.mediaFragment.setData(it.mediaUrls, it.media)
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
        viewPager.offscreenPageLimit = 4
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = when (pos) { 
                0 -> "Details"
                1 -> "Pricing/Specs"
                2 -> "Amenities"
                else -> "Media"
            }
        }.attach()
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
        val amenities = pagerAdapter.amenitiesFragment.getData()
        val pricing = pagerAdapter.pricingFragment.getData()
        val mediaImages = pagerAdapter.mediaFragment.getSelectedImages()
        val mediaVideos = pagerAdapter.mediaFragment.getSelectedVideos()
        val user = sessionManager.getUser()

        CoroutineScope(Dispatchers.IO).launch {
            val processedMedia = (mediaImages + mediaVideos).mapNotNull { uri ->
                if (uri.scheme?.startsWith("http") == true) {
                    uri.toString().substringAfter("property/")
                } else {
                    val b64 = FileUtils.encodeUriToBase64(requireContext(), uri)
                    if (b64 != null) {
                        (if (mediaVideos.contains(uri)) "data:video/mp4;base64," else "data:image/jpeg;base64,") + b64
                    } else null
                }
            }
            
            val property = PropertyDTO(
                propertyId = propertyToEdit?.propertyId, 
                landlordId = user?.userId, 
                title = details["title"] as String, 
                description = details["description"] as String, 
                bedrooms = (pricing["rooms"] as? String)?.filter { it.isDigit() }?.toIntOrNull() ?: 0, 
                bathrooms = pricing["bathrooms"] as? Double ?: 1.0, 
                areaSqft = pricing["area"] as? Int ?: 0, 
                status = "available", 
                pricePerMonth = (pricing["price"] as? String)?.toDoubleOrNull() ?: 0.0, 
                latitude = pricing["latitude"] as? Double,
                longitude = pricing["longitude"] as? Double,
                city = "Nagpur", 
                state = "Maharashtra", 
                country = "India", 
                amenityIds = amenities["amenity_ids"] as? List<Int>, 
                mediaUrls = processedMedia,
                floorId = details["floor_id"] as? Int,
                facingId = details["facing_id"] as? Int,
                roadSizeId = details["roadsize_id"] as? Int,
                proTypeId = details["protype_id"] as? Int,
                statusId = details["status_id"] as? Int,
                statusDate = details["status_date"] as? String
            )
            
            try {
                val resp = if (property.propertyId != null) RetrofitInstance.api.updateProperty(property) else RetrofitInstance.api.submitProperty(property)
                withContext(Dispatchers.Main) {
                    if (resp.status == "success") { 
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
        val amenitiesFragment = PropertyAmenitiesFragment()
        val mediaFragment = PropertyMediaFragment()
        
        override fun getItemCount(): Int = 4
        override fun createFragment(pos: Int): Fragment = when (pos) { 
            0 -> detailsFragment
            1 -> pricingFragment
            2 -> amenitiesFragment
            else -> mediaFragment
        }
    }
}
