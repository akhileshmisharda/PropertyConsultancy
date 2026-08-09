package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.example.propertyconsultancy.ui.adapters.MasonryPropertyAdapter
import kotlinx.coroutines.launch

class UpgradeFragment : Fragment() {

    private lateinit var masonryAdapter: MasonryPropertyAdapter
    private lateinit var rvProjects: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_upgrade, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.updateTitle("Premium Projects")
        
        rvProjects = view.findViewById(R.id.rvUpcomingProjects)
        
        // 1. Setup Staggered Grid (2 Columns, Vertical)
        rvProjects.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        
        masonryAdapter = MasonryPropertyAdapter(emptyList()) { property ->
            (activity as? MainActivity)?.openPropertyExplore(property)
        }
        rvProjects.adapter = masonryAdapter

        // 2. Fetch Data from server (search_properties.php)
        fetchPremiumProperties()
    }

    private fun fetchPremiumProperties() {
        lifecycleScope.launch {
            try {
                // Fetching properties from search_properties.php with a high limit
                val response = RetrofitInstance.api.getProperties(limit = 20)
                if (response.status == "success") {
                    val properties = response.data ?: emptyList()
                    masonryAdapter.updateData(properties)
                    
                    if (properties.isEmpty()) {
                        rvProjects.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("[Masonry]", "Error: ${e.message}")
                Toast.makeText(context, "Error loading projects", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
