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
        
        val imgName = arguments?.getString("TRANSITION_IMAGE_NAME")
        val titleName = arguments?.getString("TRANSITION_TITLE_NAME")
        
        if (imgName != null) view.findViewById<View>(R.id.vpExploreMedia).transitionName = imgName
        if (titleName != null) view.findViewById<View>(R.id.tvExploreTitle).transitionName = titleName
        
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
        val tvStats = view.findViewById<TextView>(R.id.tvExploreStats)
        val tvDescription = view.findViewById<TextView>(R.id.tvExploreDescription)
        val btnInterested = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInterested)

        tvTitle.text = property.title
        tvPrice.text = "₹${property.pricePerMonth?.toInt()}/mo"
        tvLocation.text = "${property.addressLine1}, ${property.city}"
        tvStats.text = "${property.bedrooms} BHK | ${property.areaSqft} sqft"
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
