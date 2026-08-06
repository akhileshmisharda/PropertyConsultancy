package com.example.propertyconsultancy.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.propertyconsultancy.data.dto.PropertyDTO

class SearchViewModel : ViewModel() {
    var lastSearchCity: String = ""
    var searchResults: List<PropertyDTO> = emptyList()
    
    // Pagination
    var currentPage: Int = 0
    var totalCount: Int = 0
    
    // Filters
    var minPrice: Double? = null
    var maxPrice: Double? = null
    var bedrooms: Int? = null
    var bathrooms: Double? = null
    
    var selectedFloorIds: List<Int> = emptyList()
    var selectedFacingIds: List<Int> = emptyList()
    var selectedRoadSizeIds: List<Int> = emptyList()
    var selectedProTypeIds: List<Int> = emptyList()
    
    var isFold1Visible: Boolean = false
    var isFold2Visible: Boolean = false
    var isMainFilterVisible: Boolean = true
    
    var lastClickedPosition: Int = -1
}
