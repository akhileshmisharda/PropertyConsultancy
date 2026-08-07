package com.example.propertyconsultancy.ui.fragments

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.propertyconsultancy.utils.UrlUtils
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
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AddPropertyFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = android.transition.TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: PropertyPagerAdapter

    private lateinit var llMediaPreview: LinearLayout
    private lateinit var llMediaIndicators: LinearLayout
    private val selectedImages = mutableListOf<Uri>()
    private val selectedVideos = mutableListOf<Uri>()

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

        initMediaViews(view)
        setupViewPager(view)
        refreshMediaPreview()
        fetchCategoriesBackground()
        
        // view.findViewById<View>(R.id.btnBack)?.setOnClickListener { requireActivity().onBackPressed() }

        view.findViewById<View>(R.id.btnSubmit).setOnClickListener {
            if (pagerAdapter.pricingFragment.validate()) {
                if (propertyToEdit != null && !hasChanges()) {
                    Toast.makeText(requireContext(), "No changes detected", Toast.LENGTH_SHORT).show()
                } else {
                    submitProperty()
                }
            }
        }

        if (propertyToEdit != null) {
            // view.findViewById<TextView>(R.id.tvMediaTitle).text = "Edit Property :-"
            // view.findViewById<Button>(R.id.btnSubmit).text = "Update"
            loadMedia(propertyToEdit!!)
            
            Handler(Looper.getMainLooper()).postDelayed({
                propertyToEdit?.let { 
                    pagerAdapter.detailsFragment.setData(it)
                    pagerAdapter.amenitiesFragment.setData(it)
                    pagerAdapter.pricingFragment.setData(it)
                }
            }, 500)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.setBottomNavVisibility(true)
    }

    private fun loadMedia(property: PropertyDTO) {
        fun processUrl(url: String, isVideo: Boolean) {
            try {
                val finalUrl = UrlUtils.getPropertyImageUrl(url) ?: return
                val uri = Uri.parse(finalUrl)
                if (isVideo) selectedVideos.add(uri) else selectedImages.add(uri)
            } catch (e: Exception) {}
        }
        property.media?.forEach { processUrl(it.fileUrl, it.mediaType == "video") }
        if (property.media.isNullOrEmpty()) {
            property.mediaUrls?.forEach { url -> processUrl(url, url.contains(".mp4") || url.contains(".mkv")) }
        }
        refreshMediaPreview()
    }

    private fun initMediaViews(view: View) {
        llMediaPreview = view.findViewById(R.id.llMediaPreview)
        llMediaIndicators = view.findViewById(R.id.llMediaIndicators)
        layoutProgress = view.findViewById(R.id.layoutProgress)
        progressBar = view.findViewById(R.id.progressBar)
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent)
    }

    private fun setupViewPager(view: View) {
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        pagerAdapter = PropertyPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 3
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = when (pos) { 0 -> "Details"; 1 -> "Pricing/Specs"; else -> "Amenities" }
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

    private fun refreshMediaPreview() {
        llMediaPreview.removeAllViews()
        llMediaIndicators.removeAllViews()
        selectedImages.forEach { addMediaPreview(it, false); addIndicator(false) }
        selectedVideos.forEach { addMediaPreview(it, true); addIndicator(true) }
        addPlusButtonCard()
    }

    private fun addIndicator(isVideo: Boolean) {
        val indicator = ImageView(requireContext())
        val size = (12 * resources.displayMetrics.density).toInt()
        val params = LinearLayout.LayoutParams(size, size).apply { setMargins(8, 0, 8, 0) }
        indicator.layoutParams = params
        indicator.setImageResource(if (isVideo) android.R.drawable.ic_media_play else android.R.drawable.presence_online)
        indicator.setColorFilter(if (isVideo) 0xFFFF5252.toInt() else 0xFF448AFF.toInt())
        llMediaIndicators.addView(indicator)
    }

    private fun addPlusButtonCard() {
        val frameLayout = FrameLayout(requireContext())
        val params = LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.MATCH_PARENT)
        params.setMargins(8, 8, 8, 8)
        frameLayout.layoutParams = params
        frameLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        val innerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        val plusIcon = TextView(requireContext()).apply { text = "+"; textSize = 48f; setTextColor(0xFF444444.toInt()); gravity = Gravity.CENTER }
        innerLayout.addView(plusIcon)
        frameLayout.addView(innerLayout)
        frameLayout.setOnClickListener { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
        llMediaPreview.addView(frameLayout)
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(100)) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val mimeType = requireContext().contentResolver.getType(uri)
                if (mimeType?.startsWith("video") == true) {
                    val size = getFileSize(uri)
                    if (size > 50 * 1024 * 1024) { // 50MB limit
                        Toast.makeText(requireContext(), "Video too large (max 50MB)", Toast.LENGTH_SHORT).show()
                    } else if (selectedVideos.size < 3) {
                        selectedVideos.add(uri)
                    }
                }
                else { if (selectedImages.size < 5) selectedImages.add(uri) }
            }
            refreshMediaPreview()
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            requireContext().contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun addMediaPreview(uri: Uri, isVideo: Boolean) {
        val frameLayout = FrameLayout(requireContext())
        val params = LinearLayout.LayoutParams(500, LinearLayout.LayoutParams.MATCH_PARENT).apply { setMargins(8, 8, 8, 8) }
        frameLayout.layoutParams = params
        val imageView = ImageView(requireContext()).apply { 
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP 
        }
        if (isVideo) {
            try {
                val retriever = MediaMetadataRetriever(); retriever.setDataSource(requireContext(), uri)
                imageView.setImageBitmap(retriever.getFrameAtTime(1000000)); retriever.release()
            } catch (e: Exception) { imageView.setImageResource(android.R.drawable.ic_menu_slideshow) }
        } else {
            imageView.load(uri)
        }
        frameLayout.addView(imageView)
        val deleteIcon = ImageView(requireContext()).apply {
            setImageResource(android.R.drawable.ic_menu_delete); setColorFilter(0xFFFF5252.toInt())
            layoutParams = FrameLayout.LayoutParams(80, 80).apply { gravity = Gravity.TOP or Gravity.END; setMargins(8, 8, 8, 8) }
            setOnClickListener { if (isVideo) selectedVideos.remove(uri) else selectedImages.remove(uri); refreshMediaPreview() }
        }
        frameLayout.addView(deleteIcon); llMediaPreview.addView(frameLayout)
    }

    private fun hasChanges(): Boolean { return true }

    private fun submitProperty() {
        layoutProgress.visibility = View.VISIBLE; tvProgressPercent.text = "0%"
        val details = pagerAdapter.detailsFragment.getData(); val amenities = pagerAdapter.amenitiesFragment.getData(); val pricing = pagerAdapter.pricingFragment.getData(); val user = sessionManager.getUser()
        CoroutineScope(Dispatchers.IO).launch {
            val processedMedia = (selectedImages + selectedVideos).mapNotNull { uri ->
                if (uri.scheme?.startsWith("http") == true) {
                    uri.toString().substringAfter("property/")
                } else {
                    val b64 = FileUtils.encodeUriToBase64(requireContext(), uri)
                    if (b64 != null) {
                        (if (selectedVideos.contains(uri)) "data:video/mp4;base64," else "data:image/jpeg;base64,") + b64
                    } else null
                }
            }
            if (processedMedia.size < (selectedImages.size + selectedVideos.size)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Some files were too large and were skipped", Toast.LENGTH_LONG).show()
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
            Log.d("[php_debug]", "submitProperty Request: $property")
            try {
                val resp = if (property.propertyId != null) RetrofitInstance.api.updateProperty(property) else RetrofitInstance.api.submitProperty(property)
                Log.d("[php_debug]", "submitProperty Response: $resp")
                withContext(Dispatchers.Main) {
                    if (resp.status == "success") { 
                        Toast.makeText(requireContext(), "Property saved successfully!", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressed() 
                    }
                    else { layoutProgress.visibility = View.GONE; Toast.makeText(requireContext(), resp.message, Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) { 
                Log.e("[php_debug]", "submitProperty Error: ${e.message}")
                withContext(Dispatchers.Main) { 
                    layoutProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } 
            }
        }
    }

    private class PropertyPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        val detailsFragment = PropertyDetailsFragment(); val amenitiesFragment = PropertyAmenitiesFragment(); val pricingFragment = PropertyPricingFragment()
        override fun getItemCount(): Int = 3
        override fun createFragment(pos: Int): Fragment = when (pos) { 0 -> detailsFragment; 1 -> pricingFragment; else -> amenitiesFragment }
    }
}
