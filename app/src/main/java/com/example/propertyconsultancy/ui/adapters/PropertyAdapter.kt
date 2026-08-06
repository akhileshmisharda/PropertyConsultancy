package com.example.propertyconsultancy.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.cache.CategoryCache
import com.example.propertyconsultancy.data.dto.PropertyDTO

class PropertyAdapter(
    private var properties: List<PropertyDTO>,
    private val isSubscribed: Boolean = true,
    private val onEdit: ((PropertyDTO, View, View) -> Unit)? = null
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivPropertyImage)
        val tvFreeListing: TextView = view.findViewById(R.id.tvFreeListing)
        val tvType: TextView = view.findViewById(R.id.tvPropertyType)
        val tvTitle: TextView = view.findViewById(R.id.tvPropertyTitle)
        val tvLocation: TextView = view.findViewById(R.id.tvPropertyLocation)
        val tvPrice: TextView = view.findViewById(R.id.tvPropertyPrice)
        val tvStatus: TextView = view.findViewById(R.id.tvPropertyStatus)
        val btnUpdate: View = view.findViewById(R.id.btnUpdateProperty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = properties[position]
        val context = holder.itemView.context
        
        holder.tvFreeListing.visibility = if (isSubscribed) View.GONE else View.VISIBLE
        
        // Property Type mapping from cache
        val categories = CategoryCache.getCategories(context)
        val typeName = property.categoryId?.let { id ->
            categories?.flatMap { it.options }?.find { it.categoryId == id }?.option
        } ?: "Property"
        holder.tvType.text = "$typeName on Rent"
        
        holder.tvTitle.text = property.title ?: "Untitled"
        holder.tvLocation.text = "${property.city ?: "N/A"}, ${property.state ?: "N/A"}"
        
        val price = property.pricePerMonth?.toInt() ?: 0
        holder.tvPrice.text = "Rent: ₹$price"
        
        // Status Styling
        val status = property.status?.lowercase() ?: "unknown"
        holder.tvStatus.text = status.replaceFirstChar { it.uppercase() }
        
        if (status == "available" || status == "active") {
            holder.tvStatus.setTextColor(Color.parseColor("#1B5E20")) // Dark Green
            holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tick, 0, 0, 0)
        } else {
            holder.tvStatus.setTextColor(Color.RED)
            holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }

        val primaryImage = property.media?.find { it.isPrimary == 1 }?.fileUrl
            ?: property.media?.firstOrNull()?.fileUrl
            ?: property.mediaUrls?.firstOrNull()
        
        val imageUrl = if (primaryImage != null && !primaryImage.startsWith("http")) {
            "http://fabkraft.in/property/$primaryImage"
        } else {
            primaryImage
        }

        holder.ivImage.load(imageUrl ?: "https://via.placeholder.com/100")
        
        if (onEdit != null) {
            holder.btnUpdate.visibility = View.VISIBLE
            
            holder.ivImage.transitionName = "edit_property_image_$position"
            holder.tvTitle.transitionName = "edit_property_title_$position"
            
            holder.btnUpdate.setOnClickListener { onEdit.invoke(property, holder.ivImage, holder.tvTitle) }
        } else {
            holder.btnUpdate.visibility = View.GONE
        }
        
        // Remove item click to satisfy "not the list item" request
        holder.itemView.setOnClickListener(null)
    }

    override fun getItemCount(): Int = properties.size

    fun updateData(newList: List<PropertyDTO>) {
        properties = newList
        notifyDataSetChanged()
    }
}
