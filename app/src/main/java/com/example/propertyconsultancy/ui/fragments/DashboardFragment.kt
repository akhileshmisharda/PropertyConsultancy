package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.ui.activities.MainActivity
import com.example.propertyconsultancy.ui.adapters.PropertyAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import com.example.propertyconsultancy.data.dto.PropertyDTO

class DashboardFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var rvMyListings: RecyclerView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvActiveCount: TextView
    private lateinit var propertyAdapter: PropertyAdapter
    private lateinit var dashboardProgress: LinearProgressIndicator

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_landlord_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        (activity as? MainActivity)?.updateTitle("Dashboard")
        initViews(view)
    }

    override fun onResume() {
        super.onResume()
        fetchProperties()
    }

    private fun initViews(view: View) {
        rvMyListings = view.findViewById(R.id.rvMyListings)
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvActiveCount = view.findViewById(R.id.tvActiveCount)
        dashboardProgress = view.findViewById(R.id.dashboardProgress)
        
        view.findViewById<View>(R.id.toolbar)?.visibility = View.GONE

        val user = sessionManager.getUser()
        val isSubscribed = user?.status == "active"

        rvMyListings.layoutManager = LinearLayoutManager(requireContext())
        propertyAdapter = PropertyAdapter(emptyList(), isSubscribed) { property, img, title ->
            (activity as? MainActivity)?.openAddProperty(property, img, title)
        }
        rvMyListings.adapter = propertyAdapter

        // Load Cached Data
        loadCachedData()

        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            (activity as? MainActivity)?.openAddProperty(null)
        }
    }

    private fun loadCachedData() {
        val cached = sessionManager.getDashboardData()
        if (cached != null) {
            try {
                val type = object : TypeToken<List<PropertyDTO>>() {}.type
                val properties: List<PropertyDTO> = Gson().fromJson(cached, type)
                updateUI(properties)
            } catch (e: Exception) {
                Log.e("[Dashboard]", "Cache Error: ${e.message}")
            }
        }
    }

    private fun updateUI(properties: List<PropertyDTO>) {
        tvTotalCount.text = properties.size.toString()
        tvActiveCount.text = properties.count { it.status == "available" || it.status == "active" }.toString()
        propertyAdapter.updateData(properties)
    }

    private fun fetchProperties() {
        val user = sessionManager.getUser() ?: return
        dashboardProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getPropertiesByUser(user.userId)
                if (response.status == "success") {
                    val properties = response.data ?: emptyList()
                    sessionManager.saveDashboardData(Gson().toJson(properties))
                    updateUI(properties)
                }
            } catch (e: Exception) {
                Log.e("[php_debug]", "Error: ${e.message}")
            } finally {
                dashboardProgress.visibility = View.GONE
            }
        }
    }
}
