package com.example.propertyconsultancy.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.PropertyDTO

class SearchPropertyAdapter(
    private var properties: List<PropertyDTO>,
    private val onItemClick: (PropertyDTO, View, View) -> Unit,
    private val onFilterClick: (String, Any) -> Unit,
    private val onChatClick: (PropertyDTO) -> Unit
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
        val btnCallLeft: View = view.findViewById(R.id.btnCallLeft)
        val btnChatLeft: View = view.findViewById(R.id.btnChatLeft)
        val btnStarLeft: View = view.findViewById(R.id.btnStarLeft)
        val tvImgCountLeft: TextView = view.findViewById(R.id.tvImgCountLeft)
        val ivVideoIconLeft: ImageView = view.findViewById(R.id.ivVideoIconLeft)

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
        val btnCallRight: View = view.findViewById(R.id.btnCallRight)
        val btnChatRight: View = view.findViewById(R.id.btnChatRight)
        val btnStarRight: View = view.findViewById(R.id.btnStarRight)
        val tvImgCountRight: TextView = view.findViewById(R.id.tvImgCountRight)
        val ivVideoIconRight: ImageView = view.findViewById(R.id.ivVideoIconRight)
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

        if (isLeft) {
            holder.layoutLeft.visibility = View.VISIBLE
            holder.layoutRight.visibility = View.GONE
            bindData(property, holder.tvTitleLeft, holder.tvPriceLeft, holder.tvLocationLeft, holder.tvBhkLeft, holder.tvBathLeft, holder.tvAreaLeft, holder.tvInterestedLeft, holder.tvAmenitiesLeft, holder.ivImageLeft, holder.btnCallLeft, holder.btnChatLeft, holder.btnStarLeft, holder.tvImgCountLeft, holder.ivVideoIconLeft)
            
            holder.ivImageLeft.transitionName = "property_image_$position"
            holder.tvTitleLeft.transitionName = "property_title_$position"
            
            setupImageCycle(property, holder.ivImageLeft, position)
            holder.itemView.setOnClickListener { onItemClick(property, holder.ivImageLeft, holder.tvTitleLeft) }
        } else {
            holder.layoutLeft.visibility = View.GONE
            holder.layoutRight.visibility = View.VISIBLE
            bindData(property, holder.tvTitleRight, holder.tvPriceRight, holder.tvLocationRight, holder.tvBhkRight, holder.tvBathRight, holder.tvAreaRight, holder.tvInterestedRight, holder.tvAmenitiesRight, holder.ivImageRight, holder.btnCallRight, holder.btnChatRight, holder.btnStarRight, holder.tvImgCountRight, holder.ivVideoIconRight)
            
            holder.ivImageRight.transitionName = "property_image_$position"
            holder.tvTitleRight.transitionName = "property_title_$position"
            
            setupImageCycle(property, holder.ivImageRight, position)
            holder.itemView.setOnClickListener { onItemClick(property, holder.ivImageRight, holder.tvTitleRight) }
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
                    val fullUrl = if (!imageUrl.startsWith("http")) "http://fabkraft.in/property/$imageUrl" else imageUrl
                    
                    // Simple and reliable fade transition
                    ivImage.animate().alpha(0f).setDuration(600).withEndAction {
                        ivImage.load(fullUrl) {
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

    private fun bindData(property: PropertyDTO, tvTitle: TextView, tvPrice: TextView, tvLocation: TextView, tvBhk: TextView, tvBath: TextView, tvArea: TextView, tvInterested: TextView, tvAmenities: TextView, ivImage: ImageView, btnCall: View, btnChat: View, btnStar: View, tvImgCount: TextView, ivVideoIcon: ImageView) {
        tvTitle.text = property.title?.uppercase() ?: "PREMIUM PROPERTY"
        
        val price = property.pricePerMonth ?: 0.0
        val formatter = java.text.DecimalFormat("#,###")
        tvPrice.text = "₹ ${formatter.format(price)}"
        
        tvLocation.text = "${property.city}"
        
        val bhk = property.bedrooms ?: 0
        val baths = property.bathrooms?.toInt() ?: 0
        val area = property.areaSqft ?: 0
        
        tvBhk.text = "$bhk BHK"
        tvBath.text = "$baths BathRoom"
        tvArea.text = "$area Sqft"
        
        // Dummy logic for interested people
        val interestedCount = (5..25).random()
        tvInterested.text = "$interestedCount Interested"
        tvInterested.visibility = View.VISIBLE

        val amenitiesCount = property.amenityCount ?: 0
        tvAmenities.text = "$amenitiesCount Amenities"

        // Tap to filter logic
        tvBhk.setOnClickListener { onFilterClick("BHK", bhk) }
        tvPrice.setOnClickListener { onFilterClick("Price", price) }

        val mediaUrls = property.mediaUrls ?: emptyList()
        val imagesCount = mediaUrls.filter { !it.endsWith(".mp4") && !it.endsWith(".mkv") }.size
        val hasVideo = mediaUrls.any { it.endsWith(".mp4") || it.endsWith(".mkv") }

        tvImgCount.text = if (imagesCount > 0) "•".repeat(imagesCount.coerceAtMost(5)) else ""
        ivVideoIcon.visibility = if (hasVideo) View.VISIBLE else View.GONE

        val imageUrl = property.media?.firstOrNull()?.fileUrl ?: mediaUrls.firstOrNull()
        val fullImageUrl = if (imageUrl != null && !imageUrl.startsWith("http")) {
            "http://fabkraft.in/property/$imageUrl"
        } else {
            imageUrl
        }
        ivImage.load(fullImageUrl ?: R.drawable.ic_app_logo)
        
        btnCall.setOnClickListener { android.widget.Toast.makeText(tvTitle.context, "Calling owner...", android.widget.Toast.LENGTH_SHORT).show() }
        btnChat.setOnClickListener { onChatClick(property) }
        btnStar.setOnClickListener { android.widget.Toast.makeText(tvTitle.context, "Starred!", android.widget.Toast.LENGTH_SHORT).show() }
    }

    override fun getItemCount(): Int = properties.size

    fun updateData(newList: List<PropertyDTO>) {
        properties = newList
        notifyDataSetChanged()
    }
}
