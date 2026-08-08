package com.example.propertyconsultancy.ui.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.ui.activities.LoginActivity
import com.example.propertyconsultancy.ui.activities.MainActivity
//updating... 2026-08-08 0057 Rishya..
//updating... 2026-08-08 13:32 Rishya
class SettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var sharedPrefs: android.content.SharedPreferences

    private var currentThemeId = 1
    private var colorScreenBg = "#F0F4FF"
    private var colorInputBg = "#F3F5F9"
    private var colorInputText = "#1A237E"
    private var colorInputHint = "#757575"
    private var colorInputBorder = "#2D3E7B"
    private var colorButtonBg = "#2D3E7B"
    private var colorButtonText = "#FFFFFF"
    private var colorButtonBorder = "#2D3E7B"

    private lateinit var btnScreenBg: View
    private lateinit var btnInputBg: View
    private lateinit var btnInputText: View
    private lateinit var btnInputHint: View
    private lateinit var btnInputBorder: View
    private lateinit var btnButtonBg: View
    private lateinit var btnButtonText: View
    private lateinit var btnButtonBorder: View
    
    private lateinit var layoutPreview: LinearLayout
    private lateinit var etPreview: EditText
    private lateinit var btnPreview: Button
    
    private lateinit var spinnerPageSize: Spinner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        sharedPrefs = requireActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        currentThemeId = sharedPrefs.getInt("selected_theme_id", 1)

        sessionManager.addActivityLog("Settings", "Accessed app settings", "info")

        (activity as? MainActivity)?.updateTitle("Settings")

        initViews(view)
        loadThemeData(currentThemeId)
        setupThemeSelection(view)
        setupColorEdits()

        view.findViewById<Button>(R.id.btnApply).setOnClickListener { saveTheme() }
        
        view.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
        view.findViewById<View>(R.id.layoutActions)?.visibility = View.VISIBLE
    }

    private fun initViews(view: View) {
        btnScreenBg = view.findViewById(R.id.btnEditScreenBg)
        btnInputBg = view.findViewById(R.id.btnEditInputBg)
        btnInputText = view.findViewById(R.id.btnEditInputText)
        btnInputHint = view.findViewById(R.id.btnEditInputHint)
        btnInputBorder = view.findViewById(R.id.btnEditInputBorder)
        btnButtonBg = view.findViewById(R.id.btnEditButtonBg)
        btnButtonText = view.findViewById(R.id.btnEditButtonText)
        btnButtonBorder = view.findViewById(R.id.btnEditButtonBorder)
        
        layoutPreview = view.findViewById(R.id.layoutPreview)
        etPreview = view.findViewById(R.id.etPreview)
        btnPreview = view.findViewById(R.id.btnPreview)
        
        spinnerPageSize = view.findViewById(R.id.spinnerPageSize)
        val sizes = resources.getStringArray(R.array.page_size_options)
        val currentSize = sessionManager.getPageSize().toString()
        spinnerPageSize.setSelection(sizes.indexOf(currentSize).coerceAtLeast(0))
    }

    private fun loadThemeData(themeId: Int) {
        when (themeId) {
            1 -> { colorScreenBg = "#F0F4FF"; colorInputBg = "#F3F5F9"; colorInputText = "#1A237E"; colorInputHint = "#757575"; colorInputBorder = "#2D3E7B"; colorButtonBg = "#2D3E7B"; colorButtonText = "#FFFFFF"; colorButtonBorder = "#2D3E7B" }
            2 -> { colorScreenBg = "#F1F8E9"; colorInputBg = "#E8F5E9"; colorInputText = "#1B5E20"; colorInputHint = "#757575"; colorInputBorder = "#2E7D32"; colorButtonBg = "#2E7D32"; colorButtonText = "#FFFFFF"; colorButtonBorder = "#2E7D32" }
            3 -> { colorScreenBg = "#FFF3E0"; colorInputBg = "#FFF8E1"; colorInputText = "#BF360C"; colorInputHint = "#757575"; colorInputBorder = "#E65100"; colorButtonBg = "#E65100"; colorButtonText = "#FFFFFF"; colorButtonBorder = "#E65100" }
            4 -> { colorScreenBg = "#F3E5F5"; colorInputBg = "#F3E5F5"; colorInputText = "#311B92"; colorInputHint = "#757575"; colorInputBorder = "#4A148C"; colorButtonBg = "#4A148C"; colorButtonText = "#FFFFFF"; colorButtonBorder = "#4A148C" }
            5 -> { colorScreenBg = "#FFEBEE"; colorInputBg = "#FFEBEE"; colorInputText = "#7F0000"; colorInputHint = "#757575"; colorInputBorder = "#B71C1C"; colorButtonBg = "#B71C1C"; colorButtonText = "#FFFFFF"; colorButtonBorder = "#B71C1C" }
            6 -> { colorScreenBg = "#E3F2FD"; colorInputBg = "#E1F5FE"; colorInputText = "#01579B"; colorInputHint = "#757575"; colorInputBorder = "#0D47A1"; colorButtonBg = "#0D47A1"; colorButtonText = "#FFFFFF"; colorButtonBorder = "#0D47A1" }
        }
        
        colorScreenBg = sharedPrefs.getString("custom_screen_bg_$themeId", colorScreenBg)!!
        colorInputBg = sharedPrefs.getString("custom_input_bg_$themeId", colorInputBg)!!
        colorInputText = sharedPrefs.getString("custom_input_text_$themeId", colorInputText)!!
        colorInputHint = sharedPrefs.getString("custom_input_hint_$themeId", colorInputHint)!!
        colorInputBorder = sharedPrefs.getString("custom_input_border_$themeId", colorInputBorder)!!
        colorButtonBg = sharedPrefs.getString("custom_button_bg_$themeId", colorButtonBg)!!
        colorButtonText = sharedPrefs.getString("custom_button_text_$themeId", colorButtonText)!!
        colorButtonBorder = sharedPrefs.getString("custom_button_border_$themeId", colorButtonBorder)!!
        
        updateUI()
    }

    private fun updateUI() {
        btnScreenBg.setBackgroundColor(Color.parseColor(colorScreenBg))
        btnInputBg.setBackgroundColor(Color.parseColor(colorInputBg))
        btnInputText.setBackgroundColor(Color.parseColor(colorInputText))
        btnInputHint.setBackgroundColor(Color.parseColor(colorInputHint))
        btnInputBorder.setBackgroundColor(Color.parseColor(colorInputBorder))
        btnButtonBg.setBackgroundColor(Color.parseColor(colorButtonBg))
        btnButtonText.setBackgroundColor(Color.parseColor(colorButtonText))
        btnButtonBorder.setBackgroundColor(Color.parseColor(colorButtonBorder))
        
        layoutPreview.setBackgroundColor(Color.parseColor(colorScreenBg))
        
        val inputDrawable = GradientDrawable()
        inputDrawable.setColor(Color.parseColor(colorInputBg))
        inputDrawable.setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor(colorInputBorder))
        inputDrawable.cornerRadius = 12 * resources.displayMetrics.density
        etPreview.background = inputDrawable
        etPreview.setTextColor(Color.parseColor(colorInputText))
        etPreview.setHintTextColor(Color.parseColor(colorInputHint))
        
        if (btnPreview is com.google.android.material.button.MaterialButton) {
            (btnPreview as com.google.android.material.button.MaterialButton).backgroundTintList = null
        }
        val buttonDrawable = GradientDrawable()
        buttonDrawable.setColor(Color.parseColor(colorButtonBg))
        buttonDrawable.setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor(colorButtonBorder))
        buttonDrawable.cornerRadius = 12 * resources.displayMetrics.density
        btnPreview.background = buttonDrawable
        btnPreview.setTextColor(Color.parseColor(colorButtonText))
    }

    private fun setupThemeSelection(view: View) {
        val rbs = listOf(
            view.findViewById<RadioButton>(R.id.rbTheme1),
            view.findViewById<RadioButton>(R.id.rbTheme2),
            view.findViewById<RadioButton>(R.id.rbTheme3),
            view.findViewById<RadioButton>(R.id.rbTheme4),
            view.findViewById<RadioButton>(R.id.rbTheme5),
            view.findViewById<RadioButton>(R.id.rbTheme6)
        )
        rbs.forEachIndexed { index, rb ->
            if (currentThemeId == index + 1) rb.isChecked = true
            rb.setOnClickListener {
                rbs.forEach { it.isChecked = false }; rb.isChecked = true
                currentThemeId = index + 1; loadThemeData(currentThemeId)
            }
        }
    }

    private fun setupColorEdits() {
        btnScreenBg.setOnClickListener { showExtendedColorPlate("Screen BG") { colorScreenBg = it; updateUI() } }
        btnInputBg.setOnClickListener { showExtendedColorPlate("Input BG") { colorInputBg = it; updateUI() } }
        btnInputText.setOnClickListener { showExtendedColorPlate("Input Text") { colorInputText = it; updateUI() } }
        btnInputHint.setOnClickListener { showExtendedColorPlate("Input Hint") { colorInputHint = it; updateUI() } }
        btnInputBorder.setOnClickListener { showExtendedColorPlate("Input Border") { colorInputBorder = it; updateUI() } }
        btnButtonBg.setOnClickListener { showExtendedColorPlate("Button BG") { colorButtonBg = it; updateUI() } }
        btnButtonText.setOnClickListener { showExtendedColorPlate("Button Text") { colorButtonText = it; updateUI() } }
        btnButtonBorder.setOnClickListener { showExtendedColorPlate("Button Border") { colorButtonBorder = it; updateUI() } }
    }

    private fun showExtendedColorPlate(title: String, onColorSelected: (String) -> Unit) {
        val palettes = listOf(
            listOf("#FFFFFF", "#F5F5F5", "#EEEEEE", "#E0E0E0", "#BDBDBD", "#9E9E9E", "#757575", "#616161", "#424242", "#212121", "#000000"),
            listOf("#E3F2FD", "#BBDEFB", "#90CAF9", "#64B5F6", "#42A5F5", "#2196F3", "#1E88E5", "#1976D2", "#1565C0", "#0D47A1"),
            listOf("#E8F5E9", "#C8E6C9", "#A5D6A7", "#81C784", "#66BB6A", "#4CAF50", "#43A047", "#388E3C", "#2E7D32", "#1B5E20"),
            listOf("#FFF3E0", "#FFE0B2", "#FFCC80", "#FFB74D", "#FFA726", "#FF9800", "#FB8C00", "#F57C00", "#EF6C00", "#E65100"),
            listOf("#F3E5F5", "#E1BEE7", "#CE93D8", "#BA68C8", "#AB47BC", "#9C27B0", "#8E24AA", "#7B1FA2", "#6A1B9A", "#4A148C"),
            listOf("#FFEBEE", "#FFCDD2", "#EF9A9A", "#E57373", "#EF5350", "#F44336", "#E53935", "#D32F2F", "#C62828", "#B71C1C")
        )

        val root = LinearLayout(requireContext())
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(16, 16, 16, 16)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Select $title")
            .setView(root)
            .setNeutralButton("Custom Hex") { _, _ -> showManualHexDialog(title, onColorSelected) }
            .setNegativeButton("Cancel", null)
            .create()

        palettes.forEach { palette ->
            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = android.view.Gravity.CENTER
            palette.forEach { hex ->
                val colorView = View(requireContext())
                val size = (35 * resources.displayMetrics.density).toInt()
                val params = LinearLayout.LayoutParams(size, size)
                params.setMargins(4, 4, 4, 4)
                colorView.layoutParams = params
                colorView.setBackgroundColor(Color.parseColor(hex))
                colorView.setOnClickListener { onColorSelected(hex); dialog.dismiss() }
                row.addView(colorView)
            }
            root.addView(row)
        }
        dialog.show()
    }

    private fun showManualHexDialog(title: String, onColorSelected: (String) -> Unit) {
        val input = EditText(requireContext()); input.hint = "#RRGGBB"; input.setPadding(48, 48, 48, 48)
        AlertDialog.Builder(requireContext()).setTitle("Custom Hex - $title").setView(input)
            .setPositiveButton("Apply") { _, _ ->
                try { Color.parseColor(input.text.toString().trim()); onColorSelected(input.text.toString().trim()) }
                catch (e: Exception) { Toast.makeText(requireContext(), "Invalid Color", Toast.LENGTH_SHORT).show() }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun saveTheme() {
        val selectedSize = spinnerPageSize.selectedItem.toString().toInt()
        sessionManager.savePageSize(selectedSize)

        sharedPrefs.edit().apply {
            putInt("selected_theme_id", currentThemeId)
            putString("custom_screen_bg_$currentThemeId", colorScreenBg)
            putString("custom_input_bg_$currentThemeId", colorInputBg)
            putString("custom_input_text_$currentThemeId", colorInputText)
            putString("custom_input_hint_$currentThemeId", colorInputHint)
            putString("custom_input_border_$currentThemeId", colorInputBorder)
            putString("custom_button_bg_$currentThemeId", colorButtonBg)
            putString("custom_button_text_$currentThemeId", colorButtonText)
            putString("custom_button_border_$currentThemeId", colorButtonBorder)
            apply()
        }
        Toast.makeText(requireContext(), "Theme Applied!", Toast.LENGTH_SHORT).show()
        requireActivity().recreate()
    }
}
