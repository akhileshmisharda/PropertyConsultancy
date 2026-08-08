package com.example.propertyconsultancy.ui.fragments

import android.content.Context
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
import com.example.propertyconsultancy.ui.activities.MainActivity

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
    private lateinit var ivHintStickyTheme: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_settings, container, false)
        ivHintStickyTheme = ImageView(requireContext())
        (view as? ViewGroup)?.addView(ivHintStickyTheme)
        return view
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
        
        if (sessionManager.isHintsEnabled()) {
            view.post { showAllStickyHints() }
        }
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
        // ... (standard theme loading logic)
        updateUI()
    }

    private fun updateUI() {
        btnScreenBg.setBackgroundColor(Color.parseColor(colorScreenBg))
        btnInputBg.setBackgroundColor(Color.parseColor(colorInputBg))
        // ... (rest of update UI)
    }

    private fun setupThemeSelection(view: View) {
        // ... (theme selection logic)
    }

    private fun setupColorEdits() {
        btnScreenBg.setOnClickListener { showExtendedColorPlate("Screen BG") { colorScreenBg = it; updateUI() } }
        // ...
    }

    private fun showExtendedColorPlate(title: String, onColorSelected: (String) -> Unit) {
        // ...
    }

    private fun showManualHexDialog(title: String, onColorSelected: (String) -> Unit) {
        // ...
    }

    private fun saveTheme() {
        // ...
        requireActivity().recreate()
    }

    fun showAllStickyHints() {
        val themeLayout = view?.findViewById<View>(R.id.layoutThemeSelection) ?: return
        showStickyHint(themeLayout, ivHintStickyTheme, "Personalize your experience")
    }

    fun hideAllStickyHints() {
        ivHintStickyTheme.visibility = View.GONE
    }

    private fun showStickyHint(target: View, hintView: ImageView, message: String) {
        val width = 450
        val height = 150
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#CC000000")
        canvas.drawRoundRect(0f, 0f, width.toFloat(), 100f, 20f, 20f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 26f
        paint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        canvas.drawText(message, 25f, 60f, paint)

        // Draw HUD tail
        paint.color = Color.parseColor("#E53935")
        canvas.drawCircle(width / 2f, 130f, 6f, paint)
        paint.strokeWidth = 2f
        paint.style = android.graphics.Paint.Style.STROKE
        canvas.drawLine(width / 2f, 100f, width / 2f, 130f, paint)
        
        hintView.setImageBitmap(bitmap)
        hintView.visibility = View.VISIBLE
        
        target.post {
            if (!isAdded) return@post
            val loc = IntArray(2)
            target.getLocationInWindow(loc)
            val rootLoc = IntArray(2)
            view?.getLocationInWindow(rootLoc)
            
            hintView.translationX = (loc[0] - rootLoc[0] + (target.width / 2) - (width / 2)).toFloat()
            hintView.translationY = (loc[1] - rootLoc[1] - height + 10).toFloat()
        }
    }
}
