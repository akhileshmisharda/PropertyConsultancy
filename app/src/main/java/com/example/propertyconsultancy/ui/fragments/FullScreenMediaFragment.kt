package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.ui.adapters.MediaSliderAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FullScreenMediaFragment : androidx.fragment.app.Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transition = android.transition.TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
            .setDuration(400)
        sharedElementEnterTransition = transition
        sharedElementReturnTransition = transition
        
        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_full_screen_media, container, false)
        val transitionName = arguments?.getString("TRANSITION_NAME")
        if (transitionName != null) {
            view.findViewById<View>(R.id.vpFullMedia).transitionName = transitionName
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val mediaUrls = arguments?.getStringArrayList("URLS") ?: return
        val startIndex = arguments?.getInt("START_INDEX") ?: 0
        
        val viewPager = view.findViewById<ViewPager2>(R.id.vpFullMedia)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabFullMediaDots)
        val btnClose = view.findViewById<ImageButton>(R.id.btnFullMediaClose)
        
        val adapter = MediaSliderAdapter(mediaUrls) { 
            // On click in full screen, maybe do nothing or zoom
        }
        viewPager.adapter = adapter
        viewPager.setCurrentItem(startIndex, false)
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val result = Bundle()
                result.putInt("current_position", position)
                parentFragmentManager.setFragmentResult("media_position", result)
            }
        })

        view.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                startPostponedEnterTransition()
                return true
            }
        })
        
        if (mediaUrls.size > 1) {
            TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
        } else {
            tabLayout.visibility = View.GONE
        }
        
        btnClose.setOnClickListener { 
            requireActivity().onBackPressed() 
        }
    }
}
