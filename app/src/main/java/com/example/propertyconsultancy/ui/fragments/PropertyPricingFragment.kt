package com.example.propertyconsultancy.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.propertyconsultancy.R
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import androidx.core.widget.addTextChangedListener
import com.google.android.material.chip.Chip
import android.text.Editable
import android.text.TextWatcher
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class PropertyPricingFragment : Fragment() {

    private lateinit var etPrice: TextInputEditText
    private lateinit var cbNegotiable: CheckBox
    private lateinit var sliderPrice: Slider
    private lateinit var tvLocation: TextView
    private lateinit var ivMiniMap: ImageView
    private lateinit var btnSelectLocation: Button

    private lateinit var llRooms: LinearLayout
    private lateinit var llBaths: LinearLayout
    private lateinit var etArea: TextInputEditText

    var onMapClick: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_property_pricing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etPrice = view.findViewById(R.id.etPrice)
        cbNegotiable = view.findViewById(R.id.cbNegotiable)
        sliderPrice = view.findViewById(R.id.sliderPrice)
        tvLocation = view.findViewById(R.id.tvLocationCoords)
        ivMiniMap = view.findViewById(R.id.ivMiniMap)
        btnSelectLocation = view.findViewById(R.id.btnSelectLocation)
        
        llRooms = view.findViewById(R.id.llRooms)
        llBaths = view.findViewById(R.id.llBaths)
        etArea = view.findViewById(R.id.etArea)

        setupPricingLogic()
        setupSelectionLogic(llRooms)
        setupSelectionLogic(llBaths)
        
        applyNumberFormatting(etPrice)
        applyNumberFormatting(etArea)
        
        btnSelectLocation.setOnClickListener { onMapClick?.invoke() }
    }

    private fun setupSelectionLogic(container: LinearLayout) {
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? Chip
            chip?.setOnClickListener {
                if (chip.isChecked) {
                    for (j in 0 until container.childCount) {
                        (container.getChildAt(j) as? Chip)?.let { if (it != chip) it.isChecked = false }
                    }
                }
            }
        }
    }

    private fun getSelectedValue(container: LinearLayout): String {
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? Chip
            if (chip?.isChecked == true) return chip.text.toString()
        }
        return ""
    }

    private fun setSelectedValue(container: LinearLayout, value: String) {
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? Chip
            if (chip != null) {
                if (chip.text?.toString()?.contains(value) == true) {
                    chip.isChecked = true
                } else {
                    chip.isChecked = false
                }
            }
        }
    }

    private fun setupPricingLogic() {
        sliderPrice.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val formattedPrice = formatNumber(value.toLong())
                if (etPrice.text.toString() != formattedPrice) {
                    etPrice.setText(formattedPrice)
                }
            }
        }

        etPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val cleanString = s.toString().replace(",", "")
                val value = cleanString.toFloatOrNull() ?: 0f
                if (value >= sliderPrice.valueFrom && value <= sliderPrice.valueTo) {
                    val stepSize = sliderPrice.stepSize
                    if (stepSize > 0f) {
                        val snappedValue = sliderPrice.valueFrom +
                                Math.round((value - sliderPrice.valueFrom) / stepSize) * stepSize
                        
                        if (snappedValue >= sliderPrice.valueFrom && snappedValue <= sliderPrice.valueTo) {
                            if (sliderPrice.value != snappedValue) {
                                sliderPrice.value = snappedValue
                            }
                        }
                    } else if (sliderPrice.value != value) {
                        sliderPrice.value = value
                    }
                }
            }
        })
    }

    private fun applyNumberFormatting(editText: TextInputEditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val input = s.toString()
                if (input.isEmpty()) return

                isUpdating = true
                
                val cleanString = input.replace(",", "")
                val formatted = try {
                    val number = cleanString.toLong()
                    formatNumber(number)
                } catch (e: Exception) {
                    cleanString
                }

                if (formatted != input) {
                    val selectionStart = editText.selectionStart
                    val commasBefore = input.substring(0, selectionStart).count { it == ',' }
                    
                    editText.setText(formatted)
                    
                    val commasAfter = formatted.substring(0, Math.min(selectionStart + (formatted.length - input.length), formatted.length)).count { it == ',' }
                    val newCursor = (selectionStart + (commasAfter - commasBefore)).coerceIn(0, formatted.length)
                    editText.setSelection(newCursor)
                }
                
                isUpdating = false
            }
        })
    }

    private fun formatNumber(number: Long): String {
        return DecimalFormat("#,###").format(number)
    }

    fun setData(property: com.example.propertyconsultancy.data.dto.PropertyDTO) {
        val price = property.pricePerMonth?.toLong() ?: 0L
        etPrice.setText(formatNumber(price))
        sliderPrice.value = price.toFloat()
        
        setSelectedValue(llRooms, property.bedrooms?.toString() ?: "")
        setSelectedValue(llBaths, property.bathrooms?.toInt()?.toString() ?: "")
        
        val area = property.areaSqft?.toLong() ?: 0L
        etArea.setText(formatNumber(area))
        
        updateLocation(property.latitude ?: 0.0, property.longitude ?: 0.0)
    }

    fun updateLocation(lat: Double, lng: Double) {
        tvLocation.text = "Coordinates: $lat, $lng"
        ivMiniMap.setImageResource(android.R.drawable.ic_dialog_map)
        ivMiniMap.setBackgroundColor(0xFFBBDEFB.toInt())
    }

    fun getData(): Map<String, Any?> {
        val bathroomsStr = getSelectedValue(llBaths).filter { it.isDigit() || it == '.' }
        val bathrooms = bathroomsStr.toDoubleOrNull() ?: 1.0
        val area = etArea.text.toString().replace(",", "").toIntOrNull() ?: 0
        
        return mapOf(
            "price" to etPrice.text.toString().replace(",", ""),
            "negotiable" to if (cbNegotiable.isChecked) "Yes" else "No",
            "location" to tvLocation.text.toString(),
            "rooms" to getSelectedValue(llRooms),
            "bathrooms" to bathrooms,
            "area" to area
        )
    }
    
    fun validate(): Boolean {
        val priceStr = etPrice.text.toString().replace(",", "")
        if (priceStr.isEmpty()) {
            etPrice.error = "Price required"
            return false
        }
        val areaStr = etArea.text.toString().replace(",", "")
        if (areaStr.isEmpty() || areaStr.toIntOrNull() ?: 0 == 0) {
            etArea.error = "Area required and cannot be 0"
            return false
        }
        return true
    }
}
