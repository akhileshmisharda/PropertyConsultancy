package com.example.propertyconsultancy.data.remote

import com.example.propertyconsultancy.data.dto.*
import retrofit2.http.*

interface ApiService {
    @GET("search_properties.php")
    suspend fun getProperties(
        @Query("city") city: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("bedrooms") bedrooms: Int? = null,
        @Query("bathrooms") bathrooms: Double? = null,
        @Query("floor_ids") floorIds: String? = null, // Comma separated
        @Query("roadsize_ids") roadsizeIds: String? = null,
        @Query("facing_ids") facingIds: String? = null,
        @Query("protype_ids") protypeIds: String? = null,
        @Query("limit") limit: Int? = 5,
        @Query("offset") offset: Int? = 0
    ): PropertyListResponseDTO

    @GET("get_cities.php")
    suspend fun getActiveCities(): List<String>

    @POST("submit_property.php")
    suspend fun submitProperty(@Body property: Map<String, @JvmSuppressWildcards Any?>): GenericResponseDTO

    @POST("update_property.php")
    suspend fun updateProperty(@Body property: Map<String, @JvmSuppressWildcards Any?>): GenericResponseDTO

    @GET("get_properties.php")
    suspend fun getPropertiesByUser(@Query("landlord_id") landlordId: Long): PropertyListResponseDTO

    @GET("get_categories.php")
    suspend fun getCategories(): CategoryResponseDTO

    @GET("get_amenities.php")
    suspend fun getAmenities(): AmenityResponseDTO

    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): AuthResponseDTO

    @POST("submit_user.php")
    suspend fun register(@Body request: RegisterRequest): AuthResponseDTO

    @POST("submit_user.php")
    suspend fun updateProfile(@Body user: UserDTO): AuthResponseDTO

    @GET("get_slider_image.php")
    suspend fun getSliderImages(): List<SliderImageDTO>

    @POST("submit_interest.php")
    suspend fun submitFavorite(@Body interaction: PropertyInteractionDTO): GenericResponseDTO

    @POST("submit_visitdate.php")
    suspend fun submitVisitRequest(@Body interaction: PropertyInteractionDTO): GenericResponseDTO

    @GET("get_interaction.php")
    suspend fun getInteraction(
        @Query("customer_id") customerId: Long,
        @Query("property_id") propertyId: Long
    ): InteractionResponseDTO
}
