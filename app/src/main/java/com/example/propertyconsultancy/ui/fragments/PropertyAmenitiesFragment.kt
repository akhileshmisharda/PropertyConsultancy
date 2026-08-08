package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.AmenityDTO
import com.example.propertyconsultancy.data.dto.AmenityListItem
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import com.example.propertyconsultancy.ui.adapters.AmenityListAdapter
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PropertyAmenitiesFragment : Fragment() {

    private lateinit var lvAvailableAmenities: ListView
    private lateinit var lvSelectedAmenities: ListView
    private lateinit var tvSelectedCount: TextView
    
    private val fullAmenitiesList = mutableListOf<AmenityDTO>()
    private val selectedAmenityIds = mutableListOf<Int>()
    private val collapsedCategories = mutableSetOf<String>()
    private val collapsedCategoriesSelected = mutableSetOf<String>()
    
    private var pendingAmenityIds: List<Int>? = null
    private lateinit var availableAdapter: AmenityListAdapter
    private lateinit var selectedAdapter: AmenityListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_property_amenities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lvAvailableAmenities = view.findViewById(R.id.lvAvailableAmenities)
        lvSelectedAmenities = view.findViewById(R.id.lvSelectedAmenities)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)

        setupAmenitiesSelector()
        fetchAmenities()
    }

    private fun setupAmenitiesSelector() {
        availableAdapter = AmenityListAdapter(requireContext(), mutableListOf(), false)
        selectedAdapter = AmenityListAdapter(requireContext(), mutableListOf(), true)

        lvAvailableAmenities.adapter = availableAdapter
        lvSelectedAmenities.adapter = selectedAdapter

        lvAvailableAmenities.setOnItemClickListener { _, _, position, _ ->
            val item = availableAdapter.getItem(position) ?: return@setOnItemClickListener
            if (item.isHeader) {
                val category = item.category ?: return@setOnItemClickListener
                if (collapsedCategories.contains(category)) {
                    collapsedCategories.remove(category)
                } else {
                    collapsedCategories.add(category)
                }
                refreshUI()
                return@setOnItemClickListener
            }
            
            selectedAmenityIds.add(item.id)
            refreshUI()
        }

        lvSelectedAmenities.setOnItemClickListener { _, _, position, _ ->
            val item = selectedAdapter.getItem(position) ?: return@setOnItemClickListener
            if (item.isHeader) {
                val category = item.category ?: return@setOnItemClickListener
                if (collapsedCategoriesSelected.contains(category)) {
                    collapsedCategoriesSelected.remove(category)
                } else {
                    collapsedCategoriesSelected.add(category)
                }
                refreshUI()
                return@setOnItemClickListener
            }
            
            selectedAmenityIds.remove(item.id)
            refreshUI()
        }
    }

    private fun refreshUI() {
        // 1. Prepare Available List (Categorized)
        val availableItems = mutableListOf<AmenityListItem>()
        val availableAmenities = fullAmenitiesList.filter { !selectedAmenityIds.contains(it.amenityId) }

        val groupedAvailable = availableAmenities.groupBy { it.category ?: "Others" }
        groupedAvailable.toSortedMap().forEach { (category, amenities) ->
            val isCollapsed = collapsedCategories.contains(category)
            availableItems.add(AmenityListItem(name = category.uppercase(), category = category, isHeader = true, isCollapsed = isCollapsed))
            
            if (!isCollapsed) {
                amenities.sortedBy { it.name }.forEach { amenity ->
                    availableItems.add(AmenityListItem(id = amenity.amenityId, name = amenity.name, category = category))
                }
            }
        }
        availableAdapter.updateData(availableItems)

        // 2. Prepare Selected List (Categorized as requested)
        val selectedItems = mutableListOf<AmenityListItem>()
        val selectedAmenities = fullAmenitiesList.filter { selectedAmenityIds.contains(it.amenityId) }
        
        val groupedSelected = selectedAmenities.groupBy { it.category ?: "Others" }
        groupedSelected.toSortedMap().forEach { (category, amenities) ->
            val isCollapsed = collapsedCategoriesSelected.contains(category)
            selectedItems.add(AmenityListItem(name = category.uppercase(), category = category, isHeader = true, isCollapsed = isCollapsed))
            
            if (!isCollapsed) {
                amenities.sortedBy { it.name }.forEach { amenity ->
                    selectedItems.add(AmenityListItem(id = amenity.amenityId, name = amenity.name, category = category))
                }
            }
        }
        selectedAdapter.updateData(selectedItems)

        tvSelectedCount.text = "(${selectedAmenityIds.size}/30 Selected)"
    }

    private fun fetchAmenities() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getAmenities()
                withContext(Dispatchers.Main) {
                    if (response.status == "success") {
                        fullAmenitiesList.clear()
                        fullAmenitiesList.addAll(response.data)
                        
                        if (pendingAmenityIds != null) {
                            applyPendingAmenities()
                        } else {
                            refreshUI()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("[Amenities]", "Fetch Error: ${e.message}")
            }
        }
    }

    private fun applyPendingAmenities() {
        val ids = pendingAmenityIds ?: return
        if (fullAmenitiesList.isEmpty()) return

        selectedAmenityIds.clear()
        selectedAmenityIds.addAll(ids)
        refreshUI()
        pendingAmenityIds = null
    }

    fun setData(property: com.example.propertyconsultancy.data.dto.PropertyDTO) {
        val amenIds = property.amenityIds ?: emptyList()
        pendingAmenityIds = amenIds
        if (fullAmenitiesList.isNotEmpty()) applyPendingAmenities()
    }

    fun getData(): Map<String, Any> {
        return mapOf("amenity_ids" to selectedAmenityIds)
    }
}
