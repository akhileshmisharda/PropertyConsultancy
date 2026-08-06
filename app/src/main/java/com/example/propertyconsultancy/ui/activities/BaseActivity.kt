package com.example.propertyconsultancy.ui.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.propertyconsultancy.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("ThemePrefs", android.content.Context.MODE_PRIVATE)
        val themeId = sharedPrefs.getInt("selected_theme_id", 2)
        
        val themeRes = when (themeId) {
            1 -> R.style.Theme_PropertyConsultancy_Theme1
            2 -> R.style.Theme_PropertyConsultancy_Theme2
            3 -> R.style.Theme_PropertyConsultancy_Theme3
            4 -> R.style.Theme_PropertyConsultancy_Theme4
            5 -> R.style.Theme_PropertyConsultancy_Theme5
            6 -> R.style.Theme_PropertyConsultancy_Theme6
            else -> R.style.Theme_PropertyConsultancy
        }
        setTheme(themeRes)
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        applyCustomColors()
    }

    private fun applyCustomColors() {
        val sharedPrefs = getSharedPreferences("ThemePrefs", android.content.Context.MODE_PRIVATE)
        val themeId = sharedPrefs.getInt("selected_theme_id", 2)
        
        val screenBg = sharedPrefs.getString("custom_screen_bg_$themeId", null)
        val inputBg = sharedPrefs.getString("custom_input_bg_$themeId", null)
        val inputText = sharedPrefs.getString("custom_input_text_$themeId", null)
        val inputHint = sharedPrefs.getString("custom_input_hint_$themeId", null)
        val inputBorder = sharedPrefs.getString("custom_input_border_$themeId", null)
        val buttonBg = sharedPrefs.getString("custom_button_bg_$themeId", null)
        val buttonText = sharedPrefs.getString("custom_button_text_$themeId", null)
        val buttonBorder = sharedPrefs.getString("custom_button_border_$themeId", null)

        val rootView = findViewById<View>(android.R.id.content)
        if (screenBg != null) {
            try { rootView.setBackgroundColor(Color.parseColor(screenBg)) } catch (e: Exception) {}
        }

        applyToChildren(rootView as ViewGroup, inputBg, inputText, inputHint, inputBorder, buttonBg, buttonText, buttonBorder)
    }

    private fun applyToChildren(viewGroup: ViewGroup, inputBg: String?, inputText: String?, inputHint: String?, inputBorder: String?, buttonBg: String?, buttonText: String?, buttonBorder: String?) {
        val density = resources.displayMetrics.density
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            
            // Check for specific section box IDs to apply Input BG
            val idName = try { resources.getResourceEntryName(child.id) } catch (e: Exception) { null }
            if (idName != null && (idName.startsWith("box") || idName.startsWith("llAmenitiesLabel") || idName.startsWith("tvRoomsLabel") || idName.startsWith("tvBathsLabel") || idName.startsWith("tvCategoriesLabel"))) {
                if (inputBg != null) {
                    try {
                        val drawable = GradientDrawable()
                        drawable.setColor(Color.parseColor(inputBg))
                        if (inputBorder != null) drawable.setStroke((0.25f * density).toInt().coerceAtLeast(1), Color.parseColor(inputBorder))
                        drawable.cornerRadius = 12 * density
                        child.background = drawable
                    } catch (e: Exception) {}
                }
            }

            when (child) {
                is CardView -> {
                    if (inputBg != null) {
                        try { child.setCardBackgroundColor(Color.parseColor(inputBg)) } catch (e: Exception) {}
                    }
                    applyToChildren(child, inputBg, inputText, inputHint, inputBorder, buttonBg, buttonText, buttonBorder)
                }
                is TextInputLayout -> {
                    try {
                        child.boxStrokeWidth = (0.25f * density).toInt().coerceAtLeast(1)
                        child.boxStrokeWidthFocused = (0.25f * density).toInt().coerceAtLeast(1)
                        if (inputBorder != null) {
                            child.setBoxStrokeColorStateList(ColorStateList.valueOf(Color.parseColor(inputBorder)))
                        }
                    } catch (e: Exception) {}
                    applyToChildren(child, inputBg, inputText, inputHint, inputBorder, buttonBg, buttonText, buttonBorder)
                }
                is EditText -> {
                    try {
                        // Do not set background for EditText inside TextInputLayout as it breaks Material design
                        if (child.parent?.parent !is TextInputLayout) {
                            val drawable = GradientDrawable()
                            drawable.setColor(if (inputBg != null) Color.parseColor(inputBg) else Color.TRANSPARENT)
                            drawable.setStroke((0.25f * density).toInt().coerceAtLeast(1), if (inputBorder != null) Color.parseColor(inputBorder) else Color.LTGRAY)
                            drawable.cornerRadius = 12 * density
                            child.background = drawable
                        }
                        
                        if (inputText != null) child.setTextColor(Color.parseColor(inputText))
                        if (inputHint != null) child.setHintTextColor(Color.parseColor(inputHint))
                    } catch (e: Exception) {}
                }
                is Button -> {
                    try {
                        if (child is MaterialButton) {
                            if (buttonBg != null) {
                                child.backgroundTintList = ColorStateList.valueOf(Color.parseColor(buttonBg))
                            }
                            if (buttonBorder != null) {
                                child.strokeColor = ColorStateList.valueOf(Color.parseColor(buttonBorder))
                                child.strokeWidth = (0.25f * density).toInt().coerceAtLeast(1)
                            }
                            if (buttonText != null) {
                                child.setTextColor(Color.parseColor(buttonText))
                            }
                            child.cornerRadius = (12 * density).toInt()
                        } else {
                            val drawable = GradientDrawable()
                            if (buttonBg != null) drawable.setColor(Color.parseColor(buttonBg))
                            if (buttonBorder != null) drawable.setStroke((0.25f * density).toInt().coerceAtLeast(1), Color.parseColor(buttonBorder))
                            drawable.cornerRadius = 12 * density
                            child.background = drawable
                            if (buttonText != null) child.setTextColor(Color.parseColor(buttonText))
                        }
                    } catch (e: Exception) {}
                }
                is ViewGroup -> applyToChildren(child, inputBg, inputText, inputHint, inputBorder, buttonBg, buttonText, buttonBorder)
            }
        }
    }
}
