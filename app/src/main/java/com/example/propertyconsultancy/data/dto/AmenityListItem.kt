package com.example.propertyconsultancy.data.dto

data class AmenityListItem(
    val id: Int = -1,
    val name: String,
    val category: String? = null,
    val isHeader: Boolean = false,
    val isCollapsed: Boolean = false
)
