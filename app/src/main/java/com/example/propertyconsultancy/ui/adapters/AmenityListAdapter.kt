package com.example.propertyconsultancy.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.AmenityListItem

class AmenityListAdapter(
    context: Context,
    private var displayList: MutableList<AmenityListItem>,
    private val isSelectedList: Boolean
) : ArrayAdapter<AmenityListItem>(context, R.layout.item_amenity, displayList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)!!
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_amenity, parent, false)
        
        val tvName = view.findViewById<TextView>(R.id.tvAmenityName)
        val ivToggle = view.findViewById<ImageView>(R.id.ivToggle)
        
        tvName.text = item.name
        val density = context.resources.displayMetrics.density
        
        if (item.isHeader) {
            view.setBackgroundColor(Color.parseColor("#F1F3F4"))
            tvName.setTextColor(Color.parseColor("#5F6368"))
            tvName.setTypeface(null, Typeface.BOLD)
            tvName.textSize = 10f
            tvName.setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            
            ivToggle.visibility = View.VISIBLE
            // Use modern chevron icons
            ivToggle.setImageResource(if (item.isCollapsed) android.R.drawable.ic_input_add else android.R.drawable.button_onoff_indicator_off)
            
            // Standardizing on a cleaner plus/minus or chevron. Let's try rotation for modern look
            ivToggle.setImageResource(R.drawable.ic_arrow_up)
            ivToggle.rotation = if (item.isCollapsed) 180f else 0f
            ivToggle.alpha = 0.5f
            
            tvName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
            val drawable = GradientDrawable()
            drawable.cornerRadius = 6 * density
            tvName.setTypeface(null, Typeface.NORMAL)
            tvName.textSize = 13f
            ivToggle.visibility = View.GONE

            if (isSelectedList) {
                // Modern Tonal Selection Style
                val typedValue = android.util.TypedValue()
                val theme = context.theme
                
                val colorContainer = if (theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)) typedValue.data else Color.parseColor("#E3F2FD")
                val onContainer = if (theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, typedValue, true)) typedValue.data else Color.parseColor("#1976D2")
                val colorOutline = if (theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)) typedValue.data else Color.parseColor("#2196F3")

                drawable.setColor(colorContainer)
                drawable.setStroke((1 * density).toInt(), colorOutline)
                tvName.setTextColor(onContainer)
                tvName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tick, 0, 0, 0)
                tvName.compoundDrawablePadding = (8 * density).toInt()
                tvName.compoundDrawableTintList = ColorStateList.valueOf(onContainer)
            } else {
                drawable.setColor(Color.parseColor("#F8F9FA")) 
                drawable.setStroke((1 * density).toInt(), Color.parseColor("#E0E0E0"))
                tvName.setTextColor(Color.parseColor("#333333"))
                tvName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            tvName.background = drawable
        }
        
        return view
    }

    fun updateData(newList: List<AmenityListItem>) {
        this.clear()
        this.addAll(newList)
        notifyDataSetChanged()
    }
}
