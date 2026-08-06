package com.example.propertyconsultancy.data.dto

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserDTO(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String, // 'tenant', 'landlord', 'both', 'admin'
    @SerializedName("profile_image_url") val profileImageUrl: String?,
    @SerializedName("is_verified") val isVerified: Int,
    @SerializedName("mobile_verified") val mobileVerified: Int = 0,
    @SerializedName("email_verified") val emailVerified: Int = 0,
    @SerializedName("status") val status: String,
    @SerializedName("address_line_1") val addressLine1: String? = null,
    @SerializedName("address_line_2") val addressLine2: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("zip_code") val zipCode: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) : Serializable

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "tenant",
    @SerializedName("profile_image_url") val profileImageUrl: String? = null,
    @SerializedName("address_line_1") val addressLine1: String? = null,
    @SerializedName("address_line_2") val addressLine2: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("zip_code") val zipCode: String? = null
)

data class AuthResponseDTO(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: UserDTO? = null
)

data class CategoryOptionDTO(
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("option") val option: String,
    @SerializedName("hasvalue") val hasValue: Int = 0,
    @SerializedName("hascaption") val hasCaption: String? = null
)

data class CategoryGroupDTO(
    @SerializedName("name") val name: String,
    @SerializedName("options") val options: List<CategoryOptionDTO>
)

data class CategoryResponseDTO(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<CategoryGroupDTO>,
    @SerializedName("message") val message: String? = null,
    @SerializedName("debug_tag") val debugTag: String? = null
)

data class PropertyDTO(
    @SerializedName("property_id") val propertyId: Long? = null,
    @SerializedName("landlord_id") val landlordId: Long? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("address_line_1") val addressLine1: String? = null,
    @SerializedName("address_line_2") val addressLine2: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("zip_code") val zipCode: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("price_per_month") val pricePerMonth: Double? = null,
    @SerializedName("cleaning_fee") val cleaningFee: Double? = null,
    @SerializedName("security_deposit") val securityDeposit: Double? = null,
    @SerializedName("bedrooms") val bedrooms: Int? = null,
    @SerializedName("bathrooms") val bathrooms: Double? = null,
    @SerializedName("max_guests") val maxGuests: Int? = null,
    @SerializedName("area_sqft") val areaSqft: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("is_featured") val isFeatured: Int? = null,
    @SerializedName("floor_id") val floorId: Int? = null,
    @SerializedName("roadsize_id") val roadSizeId: Int? = null,
    @SerializedName("facing_id") val facingId: Int? = null,
    @SerializedName("status_id") val statusId: Int? = null,
    @SerializedName("status_date") val statusDate: String? = null,
    @SerializedName("protype_id") val proTypeId: Int? = null,
    @SerializedName("media") val media: List<PropertyMediaDTO>? = null,
    @SerializedName("amenities") val amenities: List<AmenityDTO>? = null,
    @SerializedName("landlord_name") val landlordName: String? = null,
    @SerializedName("amenity_ids") val amenityIds: List<Int>? = null,
    @SerializedName("media_urls") val mediaUrls: List<String>? = null,
    @SerializedName("amenity_count") val amenityCount: Int? = null
) : Serializable

data class PropertyListResponseDTO(
    @SerializedName("status") val status: String,
    @SerializedName(value = "properties", alternate = ["data"]) val data: List<PropertyDTO>? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("message") val message: String? = null
)

data class GenericResponseDTO(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("property_id") val propertyId: Long? = null
)

data class AmenityDTO(
    @SerializedName("amenity_id") val amenityId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("icon_class") val iconClass: String?,
    @SerializedName("category") val category: String?
) : Serializable

data class AmenityResponseDTO(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<AmenityDTO>,
    @SerializedName("message") val message: String? = null,
    @SerializedName("debug_tag") val debugTag: String? = null
)

data class PropertyMediaDTO(
    @SerializedName("media_id") val mediaId: Long,
    @SerializedName("property_id") val propertyId: Long,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("is_primary") val isPrimary: Int,
    @SerializedName("display_order") val displayOrder: Int
) : Serializable

data class SliderImageDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("title") val title: String? = null
)
