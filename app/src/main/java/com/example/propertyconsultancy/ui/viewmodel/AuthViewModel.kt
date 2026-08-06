package com.example.propertyconsultancy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.propertyconsultancy.data.dto.LoginRequest
import com.example.propertyconsultancy.data.dto.RegisterRequest
import com.example.propertyconsultancy.data.dto.SliderImageDTO
import com.example.propertyconsultancy.data.dto.UserDTO
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val sessionManager: SessionManager) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _sliderImages = MutableStateFlow<List<SliderImageDTO>>(emptyList())
    val sliderImages: StateFlow<List<SliderImageDTO>> = _sliderImages

    init {
        fetchSliderImages()
    }

    private fun fetchSliderImages() {
        viewModelScope.launch {
            try {
                val images = RetrofitInstance.api.getSliderImages()
                _sliderImages.value = images
            } catch (e: Exception) {
                // Fail silently for slider
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitInstance.api.login(LoginRequest(email, password))
                if (response.status == "success" && response.user != null) {
                    sessionManager.saveUser(response.user)
                    _authState.value = AuthState.Success(response.message)
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun updateProfile(user: UserDTO) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitInstance.api.updateProfile(user)
                if (response.status == "success" && response.user != null) {
                    sessionManager.saveUser(response.user)
                    _authState.value = AuthState.Success("Profile updated successfully")
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Update failed")
            }
        }
    }

    fun register(firstName: String, lastName: String, email: String, phone: String, password: String, role: String, profileImageUrl: String?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitInstance.api.register(
                    RegisterRequest(firstName, lastName, email, phone, password, role, profileImageUrl)
                )
                if (response.status == "success") {
                    _authState.value = AuthState.Success(response.message)
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

class AuthViewModelFactory(private val sessionManager: SessionManager) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
