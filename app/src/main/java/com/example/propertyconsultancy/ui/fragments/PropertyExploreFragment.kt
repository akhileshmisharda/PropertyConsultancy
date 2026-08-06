package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.google.android.material.button.MaterialButton

class PropertyExploreFragment : Fragment() {

    private var property: PropertyDTO? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        property = arguments?.getSerializable("property") as? PropertyDTO
        
        val transition = android.transition.TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
            .setDuration(400)
            
        sharedElementEnterTransition = transition
        sharedElementReturnTransition = transition
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_property_explore, container, false)
        
        val prefixes = listOf("IMAGE", "TITLE", "PRICE", "LOCATION", "BHK", "AREA", "FACING", "ROADSIZE", "FURNISHED", "BATH", "TYPE")
        val viewIds = listOf(R.id.vpExploreMedia, R.id.tvExploreTitle, R.id.tvExplorePrice, R.id.tvExploreLocation, R.id.tvExploreBhk, R.id.tvExploreArea, R.id.tvExploreFacing, R.id.tvExploreRoadSize, R.id.tvExploreFurnished, R.id.tvExploreBath, R.id.tvExplorePropertyType)
        
        prefixes.forEachIndexed { index, prefix ->
            val transitionName = arguments?.getString("TRANSITION_PROPERTY_${prefix}_NAME")
            if (transitionName != null) {
                view.findViewById<View>(viewIds[index])?.transitionName = transitionName
            }
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.updateTitle("Property Explore")
        
        val property = property ?: return
        
        val vpMedia = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpExploreMedia)
        val tabLayout = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabMediaDots)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvExploreTitle)
        val tvPrice = view.findViewById<TextView>(R.id.tvExplorePrice)
        val tvLocation = view.findViewById<TextView>(R.id.tvExploreLocation)
        val tvBhk = view.findViewById<TextView>(R.id.tvExploreBhk)
        val tvArea = view.findViewById<TextView>(R.id.tvExploreArea)
        val tvFacing = view.findViewById<TextView>(R.id.tvExploreFacing)
        val tvRoadSize = view.findViewById<TextView>(R.id.tvExploreRoadSize)
        val tvFurnished = view.findViewById<TextView>(R.id.tvExploreFurnished)
        val tvBath = view.findViewById<TextView>(R.id.tvExploreBath)
        val tvPropertyType = view.findViewById<TextView>(R.id.tvExplorePropertyType)
        
        val tvDescription = view.findViewById<TextView>(R.id.tvExploreDescription)
        val btnInterested = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInterested)

        tvTitle.text = property.title?.uppercase()
        val formatter = java.text.DecimalFormat("#,###")
        tvPrice.text = "₹ ${formatter.format(property.pricePerMonth ?: 0.0)}/mo"
        tvLocation.text = if (property.state.isNullOrEmpty()) "${property.city}" else "${property.city}, ${property.state}"
        
        tvBhk.text = "${property.bedrooms} BHK"
        tvArea.text = "${property.areaSqft} Sqft"
        tvBath.text = "${property.bathrooms?.toInt() ?: 0} BathRoom"

        // Category mapping
        val categories = com.example.propertyconsultancy.data.cache.CategoryCache.getCategories(requireContext())
        fun getOptionName(ids: List<Int>?, group: String): String {
            if (ids.isNullOrEmpty()) return "N/A"
            val options = categories?.find { it.name.contains(group, true) }?.options
            return options?.find { it.categoryId == ids.first() }?.option ?: "ID: ${ids.first()}"
        }

        tvFacing.text = "Facing: ${getOptionName(property.facingId?.let { listOf(it) }, "Facing")}"
        tvRoadSize.text = "Road: ${getOptionName(property.roadSizeId?.let { listOf(it) }, "Road")}"
        tvPropertyType.text = getOptionName(property.proTypeId?.let { listOf(it) }, "PropertyType").uppercase()

        val isFurnished = property.description?.contains("furnished", true) == true || 
                          property.amenities?.any { it.name.contains("furnished", true) } == true
        tvFurnished.text = if (isFurnished) "Furnished" else "Unfurnished"

        tvDescription.text = property.description ?: "No description available."

        val mediaUrls = (property.media?.map { it.fileUrl } ?: emptyList()) + (property.mediaUrls ?: emptyList()).distinct()
        val adapter = com.example.propertyconsultancy.ui.adapters.MediaSliderAdapter(mediaUrls) { position ->
            val fullScreen = FullScreenMediaFragment()
            val args = Bundle()
            args.putStringArrayList("URLS", ArrayList(mediaUrls))
            args.putInt("START_INDEX", position)
            args.putString("TRANSITION_NAME", vpMedia.transitionName)
            fullScreen.arguments = args
            
            parentFragmentManager.beginTransaction()
                .addSharedElement(vpMedia, vpMedia.transitionName)
                .replace(R.id.nav_host_fragment, fullScreen, "FullScreenMedia")
                .addToBackStack(null)
                .commit()
        }
        vpMedia.adapter = adapter
        
        if (mediaUrls.size > 1) {
            com.google.android.material.tabs.TabLayoutMediator(tabLayout, vpMedia) { _, _ -> }.attach()
        } else {
            tabLayout.visibility = View.GONE
        }

        btnInterested.setOnClickListener {
            Toast.makeText(requireContext(), "Owner will contact you soon!", Toast.LENGTH_LONG).show()
        }
    }
}
