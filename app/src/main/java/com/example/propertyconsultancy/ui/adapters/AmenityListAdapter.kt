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

    private val sharedPrefs = context.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
    private val themeId = sharedPrefs.getInt("selected_theme_id", 1)
    
    private val buttonBgColor = sharedPrefs.getString("custom_button_bg_$themeId", "#2D3E7B")!!
    private val buttonTextColor = sharedPrefs.getString("custom_button_text_$themeId", "#FFFFFF")!!

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)!!
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_amenity, parent, false)
        
        val tvName = view.findViewById<TextView>(R.id.tvAmenityName)
        val ivToggle = view.findViewById<ImageView>(R.id.ivToggle)
        
        tvName.text = item.name
        val density = context.resources.displayMetrics.density
        
        if (item.isHeader) {
            view.setBackgroundColor(Color.parseColor("#F5F5F5"))
            tvName.setTextColor(Color.parseColor("#444444"))
            tvName.setTypeface(null, Typeface.BOLD)
            tvName.textSize = 11f
            
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
                drawable.setColor(Color.parseColor(buttonBgColor))
                tvName.setTextColor(Color.parseColor(buttonTextColor))
                tvName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tick, 0, 0, 0)
                tvName.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor(buttonTextColor))
            } else {
                drawable.setColor(Color.parseColor("#05000000")) // Very light gray for visibility
                tvName.setTextColor(Color.BLACK)
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
