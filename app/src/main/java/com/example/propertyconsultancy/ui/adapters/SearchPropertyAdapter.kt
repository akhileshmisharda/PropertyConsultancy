package com.example.propertyconsultancy.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.cache.CategoryCache
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.utils.UrlUtils

class SearchPropertyAdapter(
    private var properties: List<PropertyDTO>,
    private val onItemClick: (PropertyDTO, Map<String, View>) -> Unit,
    private val onFilterClick: (String, Any) -> Unit,
    private val onChatClick: (PropertyDTO) -> Unit,
    var currentCity: String? = null,
    var currentBhk: Int? = null,
    var currentMinPrice: Double? = null,
    var currentMaxPrice: Double? = null,
    var currentProTypeIds: List<Int> = emptyList()
) : RecyclerView.Adapter<SearchPropertyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Left Layout Views
        val layoutLeft: View = view.findViewById(R.id.layoutLeft)
        val ivImageLeft: ImageView = view.findViewById(R.id.ivPropertyLeft)
        val tvPriceLeft: TextView = view.findViewById(R.id.tvPriceLeft)
        val tvTitleLeft: TextView = view.findViewById(R.id.tvTitleLeft)
        val tvLocationLeft: TextView = view.findViewById(R.id.tvLocationLeft)
        val tvBhkLeft: TextView = view.findViewById(R.id.tvBhkLeft)
        val tvBathLeft: TextView = view.findViewById(R.id.tvBathLeft)
        val tvAreaLeft: TextView = view.findViewById(R.id.tvAreaLeft)
        val tvInterestedLeft: TextView = view.findViewById(R.id.tvInterestedLeft)
        val tvAmenitiesLeft: TextView = view.findViewById(R.id.tvAmenitiesLeft)
        val tvImgCountLeft: TextView = view.findViewById(R.id.tvImgCountLeft)
        val ivVideoIconLeft: ImageView = view.findViewById(R.id.ivVideoIconLeft)
        
        val tvFacingLeft: TextView = view.findViewById(R.id.tvFacingLeft)
        val tvRoadSizeLeft: TextView = view.findViewById(R.id.tvRoadSizeLeft)
        val tvFurnishedLeft: TextView = view.findViewById(R.id.tvFurnishedLeft)
        val tvPropertyTypeLeft: TextView = view.findViewById(R.id.tvPropertyTypeLeft)
        
        val ivFilterPriceLeft: View = view.findViewById(R.id.ivFilterPriceLeft)
        val ivFilterBhkLeft: View = view.findViewById(R.id.ivFilterBhkLeft)
        val ivFilterLocationLeft: View = view.findViewById(R.id.ivFilterLocationLeft)
        val ivFilterProTypeLeft: View = view.findViewById(R.id.ivFilterProTypeLeft)

        // Right Layout Views
        val layoutRight: View = view.findViewById(R.id.layoutRight)
        val ivImageRight: ImageView = view.findViewById(R.id.ivPropertyRight)
        val tvPriceRight: TextView = view.findViewById(R.id.tvPriceRight)
        val tvTitleRight: TextView = view.findViewById(R.id.tvTitleRight)
        val tvLocationRight: TextView = view.findViewById(R.id.tvLocationRight)
        val tvBhkRight: TextView = view.findViewById(R.id.tvBhkRight)
        val tvBathRight: TextView = view.findViewById(R.id.tvBathRight)
        val tvAreaRight: TextView = view.findViewById(R.id.tvAreaRight)
        val tvInterestedRight: TextView = view.findViewById(R.id.tvInterestedRight)
        val tvAmenitiesRight: TextView = view.findViewById(R.id.tvAmenitiesRight)
        val tvImgCountRight: TextView = view.findViewById(R.id.tvImgCountRight)
        val ivVideoIconRight: ImageView = view.findViewById(R.id.ivVideoIconRight)
        
        val tvFacingRight: TextView = view.findViewById(R.id.tvFacingRight)
        val tvRoadSizeRight: TextView = view.findViewById(R.id.tvRoadSizeRight)
        val tvFurnishedRight: TextView = view.findViewById(R.id.tvFurnishedRight)
        val tvPropertyTypeRight: TextView = view.findViewById(R.id.tvPropertyTypeRight)
        
        val ivFilterPriceRight: View = view.findViewById(R.id.ivFilterPriceRight)
        val ivFilterBhkRight: View = view.findViewById(R.id.ivFilterBhkRight)
        val ivFilterLocationRight: View = view.findViewById(R.id.ivFilterLocationRight)
        val ivFilterProTypeRight: View = view.findViewById(R.id.ivFilterProTypeRight)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_zigzag_property, parent, false)
        return ViewHolder(view)
    }

    private val handlers = mutableMapOf<Int, android.os.Handler>()
    private val runnables = mutableMapOf<Int, Runnable>()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val property = properties[position]
        val isLeft = position % 2 == 0
        
        // Clear previous animations/cycles for this VH
        stopImageCycle(position)

        val views = if (isLeft) {
            listOf(holder.ivImageLeft, holder.tvTitleLeft, holder.tvPriceLeft, holder.tvLocationLeft, holder.tvBhkLeft, holder.tvAreaLeft, holder.tvFacingLeft, holder.tvRoadSizeLeft, holder.tvFurnishedLeft, holder.tvBathLeft, holder.tvPropertyTypeLeft, holder.tvInterestedLeft, holder.tvAmenitiesLeft)
        } else {
            listOf(holder.ivImageRight, holder.tvTitleRight, holder.tvPriceRight, holder.tvLocationRight, holder.tvBhkRight, holder.tvAreaRight, holder.tvFacingRight, holder.tvRoadSizeRight, holder.tvFurnishedRight, holder.tvBathRight, holder.tvPropertyTypeRight, holder.tvInterestedRight, holder.tvAmenitiesRight)
        }

        val sharedElements = mutableMapOf<String, View>()
        val prefixes = listOf("property_image", "property_title", "property_price", "property_location", "property_bhk", "property_area", "property_facing", "property_roadsize", "property_furnished", "property_bath", "property_type", "property_interested", "property_amenities")
        
        views.forEachIndexed { index, view ->
            val name = "${prefixes[index]}_$position"
            view.transitionName = name
            sharedElements[name] = view
        }

        if (isLeft) {
            holder.layoutLeft.visibility = View.VISIBLE
            holder.layoutRight.visibility = View.GONE
            bindData(property, holder.tvTitleLeft, holder.tvPriceLeft, holder.tvLocationLeft, holder.tvBhkLeft, holder.tvBathLeft, holder.tvAreaLeft, holder.tvInterestedLeft, holder.tvAmenitiesLeft, holder.ivImageLeft, holder.tvImgCountLeft, holder.ivVideoIconLeft, holder.ivFilterPriceLeft, holder.ivFilterBhkLeft, holder.ivFilterLocationLeft, holder.ivFilterProTypeLeft, holder.tvFacingLeft, holder.tvRoadSizeLeft, holder.tvFurnishedLeft, holder.tvPropertyTypeLeft)
            
            setupImageCycle(property, holder.ivImageLeft, position)
            holder.itemView.setOnClickListener { onItemClick(property, sharedElements) }
        } else {
            holder.layoutLeft.visibility = View.GONE
            holder.layoutRight.visibility = View.VISIBLE
            bindData(property, holder.tvTitleRight, holder.tvPriceRight, holder.tvLocationRight, holder.tvBhkRight, holder.tvBathRight, holder.tvAreaRight, holder.tvInterestedRight, holder.tvAmenitiesRight, holder.ivImageRight, holder.tvImgCountRight, holder.ivVideoIconRight, holder.ivFilterPriceRight, holder.ivFilterBhkRight, holder.ivFilterLocationRight, holder.ivFilterProTypeRight, holder.tvFacingRight, holder.tvRoadSizeRight, holder.tvFurnishedRight, holder.tvPropertyTypeRight)
            
            setupImageCycle(property, holder.ivImageRight, position)
            holder.itemView.setOnClickListener { onItemClick(property, sharedElements) }
        }
    }

    private fun setupImageCycle(property: PropertyDTO, ivImage: ImageView, position: Int) {
        val mediaUrls = (property.media?.map { it.fileUrl } ?: emptyList()) + (property.mediaUrls ?: emptyList())
        val images = mediaUrls.filter { !it.endsWith(".mp4") && !it.endsWith(".mkv") }.distinct()
        
        if (images.size > 1) {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var currentIdx = 0
            
            val runnable = object : Runnable {
                override fun run() {
                    currentIdx = (currentIdx + 1) % images.size
                    val imageUrl = images[currentIdx]
                    
                    // Simple and reliable fade transition
                    ivImage.animate().alpha(0f).setDuration(600).withEndAction {
                        ivImage.load(UrlUtils.getPropertyImageUrl(imageUrl)) {
                            listener(
                                onSuccess = { _, _ ->
                                    ivImage.animate().alpha(1f).setDuration(600).start()
                                },
                                onError = { _, _ ->
                                    ivImage.alpha = 1f // Reset on error
                                }
                            )
                        }
                    }.start()
                    
                    handler.postDelayed(this, 7000)
                }
            }
            
            handlers[position] = handler
            runnables[position] = runnable
            
            val initialDelay = (3000..9000).random().toLong()
            handler.postDelayed(runnable, initialDelay)
        }
    }

    private fun stopImageCycle(position: Int) {
        handlers[position]?.removeCallbacks(runnables[position] ?: return)
        handlers.remove(position)
        runnables.remove(position)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        stopImageCycle(holder.bindingAdapterPosition)
    }

    private fun bindData(property: PropertyDTO, tvTitle: TextView, tvPrice: TextView, tvLocation: TextView, tvBhk: TextView, tvBath: TextView, tvArea: TextView, tvInterested: TextView, tvAmenities: TextView, ivImage: ImageView, tvImgCount: TextView, ivVideoIcon: ImageView, ivFilterPrice: View, ivFilterBhk: View, ivFilterLocation: View, ivFilterProType: View, tvFacing: TextView, tvRoadSize: TextView, tvFurnished: TextView, tvPropertyType: TextView) {
        tvTitle.text = property.title?.uppercase() ?: "PREMIUM PROPERTY"
        
        val price = property.pricePerMonth ?: 0.0
        val formatter = java.text.DecimalFormat("#,###")
        tvPrice.text = "₹ ${formatter.format(price)}"
        
        val locationText = buildString {
            if (!property.addressLine2.isNullOrEmpty()) append("${property.addressLine2}, ")
            append(property.city ?: "")
        }
        tvLocation.text = locationText
        
        val bhk = property.bedrooms ?: 0
        val baths = property.bathrooms?.toInt() ?: 0
        val area = property.areaSqft ?: 0
        
        tvBhk.text = "$bhk BHK"
        tvBath.text = "$baths Bath"
        tvArea.text = "$area Sqft"
        
        // Facing, RoadSize, Status mapping
        val categories = CategoryCache.getCategories(tvTitle.context)
        fun getOptionName(ids: List<Int>?, group: String): String {
            if (ids.isNullOrEmpty()) return "N/A"
            val cleanGroup = group.replace(" ", "").lowercase()
            val options = categories?.find {
                val name = it.name.replace(" ", "").lowercase()
                name.contains(cleanGroup) || cleanGroup.contains(name)
            }?.options
            
            val found = options?.find { it.categoryId == ids.first() }?.option
            if (found != null) return found
            
            return categories?.flatMap { it.options }?.find { it.categoryId == ids.first() }?.option ?: "ID: ${ids.first()}"
        }
        
        tvFacing.text = "Facing : ${getOptionName(property.facingId?.let { listOf(it) }, "Facing")}"
        tvRoadSize.text = "Road : ${getOptionName(property.roadSizeId?.let { listOf(it) }, "Road Size")}"
        
        // Furnished logic: check if any amenity name contains "Furnished" or check common keywords in description
        val isFurnished = property.description?.contains("furnished", true) == true || 
                          property.amenities?.any { it.name.contains("furnished", true) } == true
        tvFurnished.text = if (isFurnished) "Furnished" else "Unfurnished"
        
        tvPropertyType.text = getOptionName(property.proTypeId?.let { listOf(it) }, "Property Type")

        // Filter Indicators
        ivFilterLocation.visibility = if (!currentCity.isNullOrEmpty()) View.VISIBLE else View.GONE
        ivFilterBhk.visibility = if (currentBhk != null && currentBhk == bhk) View.VISIBLE else View.GONE
        
        val isPriceFiltered = (currentMinPrice != null && price >= currentMinPrice!!) || (currentMaxPrice != null && price <= currentMaxPrice!!)
        ivFilterPrice.visibility = if (isPriceFiltered) View.VISIBLE else View.GONE

        ivFilterProType.visibility = if (currentProTypeIds.isNotEmpty() && currentProTypeIds.contains(property.proTypeId)) View.VISIBLE else View.GONE

        // Dummy logic for interested people
        val interestedCount = (5..25).random()
        tvInterested.text = "{ $interestedCount Interested }"
        tvInterested.visibility = View.VISIBLE

        val amenitiesCount = property.amenityCount ?: 0
        tvAmenities.text = "{ $amenitiesCount Amenities }"

        // Tap to filter logic
        tvBhk.setOnClickListener { onFilterClick("BHK", bhk) }
        tvPrice.setOnClickListener { onFilterClick("Price", price) }

        val mediaUrls = property.mediaUrls ?: emptyList()
        val imagesCount = mediaUrls.filter { !it.endsWith(".mp4") && !it.endsWith(".mkv") }.size
        val hasVideo = mediaUrls.any { it.endsWith(".mp4") || it.endsWith(".mkv") }

        tvImgCount.text = if (imagesCount > 0) "•".repeat(imagesCount.coerceAtMost(5)) else ""
        ivVideoIcon.visibility = if (hasVideo) View.VISIBLE else View.GONE

        val imageUrl = property.media?.firstOrNull()?.fileUrl ?: mediaUrls.firstOrNull()
        ivImage.load(UrlUtils.getPropertyImageUrl(imageUrl) ?: R.drawable.ic_app_logo)
    }

    override fun getItemCount(): Int = properties.size

    fun updateData(newList: List<PropertyDTO>) {
        properties = newList
        notifyDataSetChanged()
    }
}
