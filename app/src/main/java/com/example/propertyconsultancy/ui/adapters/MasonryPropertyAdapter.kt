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
import com.example.propertyconsultancy.utils.UrlUtils

/**
 * Adapter for Pinterest-style Masonry layout.
 * Randomly assigns heights to items to create the staggered effect.
 */
class MasonryPropertyAdapter(
    private var items: List<PropertyDTO>,
    private val onItemClick: (PropertyDTO) -> Unit
) : RecyclerView.Adapter<MasonryPropertyAdapter.ViewHolder>() {

    // Pre-defined height options in DP to maintain consistency and prevent shifting
    private val heightOptions = listOf(160, 200, 240, 280)
    
    // Map to store assigned height for each position to ensure it remains stable
    private val positionHeights = mutableMapOf<Int, Int>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivMasonryImage)
        val tvPrice: TextView = view.findViewById(R.id.tvMasonryPrice)
        val tvTitle: TextView = view.findViewById(R.id.tvMasonryTitle)
        val rootContainer: View = view.findViewById(R.id.cardMasonry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_masonry_property, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val property = items[position]
        
        // 1. Assign/Retrieve a stable random height for this position
        val heightDp = positionHeights.getOrPut(position) { heightOptions.random() }
        val density = holder.itemView.resources.displayMetrics.density
        val heightPx = (heightDp * density).toInt()
        
        // Update the item height dynamically
        val layoutParams = holder.rootContainer.layoutParams
        layoutParams.height = heightPx
        holder.rootContainer.layoutParams = layoutParams

        // 2. Bind Data
        val imageUrl = property.media?.firstOrNull()?.fileUrl ?: property.mediaUrls?.firstOrNull()
        holder.ivImage.load(UrlUtils.getPropertyImageUrl(imageUrl))

        val formatter = java.text.DecimalFormat("#,###")
        holder.tvPrice.text = "₹ ${formatter.format(property.pricePerMonth ?: 0.0)}"
        holder.tvTitle.text = property.title?.uppercase() ?: "PREMIUM"

        holder.itemView.setOnClickListener { onItemClick(property) }
    }

    override fun getItemCount() = items.size

    fun updateData(newList: List<PropertyDTO>) {
        items = newList
        positionHeights.clear() // Clear cached heights if list changes completely
        notifyDataSetChanged()
    }
}
