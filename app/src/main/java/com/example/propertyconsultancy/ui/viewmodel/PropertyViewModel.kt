package com.example.propertyconsultancy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PropertyState {
    object Loading : PropertyState()
    data class Success(val properties: List<PropertyDTO>) : PropertyState()
    data class Error(val message: String) : PropertyState()
}

class PropertyViewModel : ViewModel() {

    private val _propertyState = MutableStateFlow<PropertyState>(PropertyState.Loading)
    val propertyState: StateFlow<PropertyState> = _propertyState

    fun fetchLandlordProperties(landlordId: Long) {
        viewModelScope.launch {
            _propertyState.value = PropertyState.Loading
            try {
                val response = RetrofitInstance.api.getPropertiesByUser(landlordId)
                Log.d("[php_debug]", "Raw Response: status=${response.status}, count=${response.count}, properties_null=${response.data == null}")
                val properties = response.data ?: emptyList()
                Log.d("[php_debug]", "Fetched ${properties.size} properties for landlord $landlordId")
                properties.forEach { 
                    Log.d("[php_debug]", "Property: ${it.title} (ID: ${it.propertyId})")
                }
                _propertyState.value = PropertyState.Success(properties)
            } catch (e: Exception) {
                Log.e("[php_debug]", "Error fetching properties: ${e.message}")
                _propertyState.value = PropertyState.Error(e.message ?: "Failed to fetch properties")
            }
        }
    }
    
    fun fetchAllProperties() {
        viewModelScope.launch {
            _propertyState.value = PropertyState.Loading
            try {
                // Using existing getProperties endpoint, assuming it returns all if no city is passed
                val response = RetrofitInstance.api.getProperties() 
                val properties = response.data ?: emptyList()
                Log.d("[php_debug]", "Fetched ${properties.size} properties for tenant view")
                properties.forEach { 
                    Log.d("[php_debug]", "Property: ${it.title} (ID: ${it.propertyId})")
                }
                _propertyState.value = PropertyState.Success(properties)
            } catch (e: Exception) {
                _propertyState.value = PropertyState.Error(e.message ?: "Failed to fetch properties")
            }
        }
    }
}
