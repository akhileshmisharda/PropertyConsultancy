package com.example.propertyconsultancy.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.local.SessionManager
import com.example.propertyconsultancy.data.dto.PropertyDTO
import com.example.propertyconsultancy.ui.fragments.DashboardFragment
import com.example.propertyconsultancy.ui.fragments.ProfileFragment
import com.example.propertyconsultancy.ui.fragments.SettingsFragment
import com.example.propertyconsultancy.ui.fragments.ActivityFragment
import com.example.propertyconsultancy.ui.fragments.SearchFragment
import com.example.propertyconsultancy.ui.fragments.AddPropertyFragment
import com.example.propertyconsultancy.ui.fragments.PropertyExploreFragment
import com.example.propertyconsultancy.ui.fragments.ChatFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.ImageButton
import android.widget.Toast
import android.view.View

class MainActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvHeaderTitle: android.widget.TextView
    private lateinit var btnHeaderBack: android.widget.ImageButton
    private lateinit var btnHeaderToggleHints: android.widget.ImageButton
    private lateinit var ivHudHintHeader: android.widget.ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        
        bottomNav = findViewById(R.id.bottomNavigation)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        btnHeaderBack = findViewById(R.id.btnHeaderBack)
        btnHeaderToggleHints = findViewById(R.id.btnHeaderToggleHints)
        ivHudHintHeader = findViewById(R.id.ivHudHintHeader)
        
        updateHintToggleIcon()
        
        btnHeaderToggleHints.setOnClickListener {
            val enabled = !sessionManager.isHintsEnabled()
            sessionManager.setHintsEnabled(enabled)
            updateHintToggleIcon()
            
            // Show HUD hint for toggle action (bypass check for this specific action so user knows it's working)
            showHudHint(if (enabled) "HUD Popups: STARTED" else "HUD Popups: STOPPED")
        }
        
        setupNavigation()

        btnHeaderBack.setOnClickListener {
            onBackPressed()
        }

        findViewById<ImageButton>(R.id.btnHeaderSettings).setOnClickListener {
            updateTitle("Settings")
            loadFragment(SettingsFragment(), "settings")
            // Deselect bottom nav if needed
            // bottomNav.selectedItemId = -1 // Material 3 might not support -1 easily
        }

        findViewById<ImageButton>(R.id.btnHeaderLogout).setOnClickListener {
            logout()
        }

        // Default fragment
        if (savedInstanceState == null) {
            val openProfile = intent.getBooleanExtra("OPEN_PROFILE", false)
            if (openProfile) {
                updateTitle("Profile")
                loadFragment(ProfileFragment(), "profile")
                Toast.makeText(this, "Welcome! Please complete your profile details.", Toast.LENGTH_LONG).show()
            } else {
                updateTitle("Dashboard")
                loadFragment(DashboardFragment(), "dashboard")
            }
        }
    }

    private fun setupNavigation() {
        bottomNav.selectedItemId = R.id.nav_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            animateExplosion(item.itemId)
            when (item.itemId) {
                R.id.nav_profile -> { updateTitle("Profile"); loadFragment(ProfileFragment(), "profile"); true }
                R.id.nav_activity -> { updateTitle("Activity"); loadFragment(ActivityFragment(), "activity"); true }
                R.id.nav_dashboard -> { updateTitle("Dashboard"); loadFragment(DashboardFragment(), "dashboard"); true }
                R.id.nav_listing -> { updateTitle("Listings"); loadFragment(SearchFragment(), "listing"); true }
                R.id.nav_upgrade -> { 
                    updateTitle("Premium")
                    Toast.makeText(this, "Premium features coming soon!", Toast.LENGTH_SHORT).show()
                    true 
                }
                else -> false
            }
        }
    }

    private fun animateExplosion(itemId: Int) {
        val itemView = bottomNav.findViewById<View>(itemId) ?: return
        val iconContainer = itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_container) ?: return
        
        // Base scales are defined in CurvedBottomNavigationView.kt (Dashboard: 1.6f, Others: 1.0f)
        val baseScale = if (itemId == R.id.nav_dashboard) 1.6f else 1.0f
        
        iconContainer.animate()
            .scaleX(baseScale * 1.4f)
            .scaleY(baseScale * 1.4f)
            .setDuration(150)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .withEndAction {
                iconContainer.animate()
                    .scaleX(baseScale)
                    .scaleY(baseScale)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    fun updateTitle(title: String) {
        tvHeaderTitle.text = title
    }

    private fun updateHintToggleIcon() {
        val enabled = sessionManager.isHintsEnabled()
        btnHeaderToggleHints.setImageResource(if (enabled) R.drawable.ic_hint_on else R.drawable.ic_hint_off)
    }

    private fun showHudHint(message: String) {
        val width = 600
        val height = 300
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val accentColor = android.graphics.Color.parseColor("#E53935")
        val hudLineColor = android.graphics.Color.parseColor("#757575")
        val textColor = android.graphics.Color.parseColor("#424242")
        
        val paintText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 30f
            color = textColor
            typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        }
        
        val textWidth = paintText.measureText(message)
        val padding = 20f
        val boxWidth = textWidth + padding * 2
        val boxHeight = 70f
        
        val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = hudLineColor
            strokeWidth = 2.5f
            style = android.graphics.Paint.Style.STROKE
        }
        
        val nodePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = android.graphics.Paint.Style.FILL
        }

        // Anchor at Top Right (pointing to toggle)
        val anchorX = width - 100f
        val anchorY = 20f
        
        val p1 = android.graphics.PointF(anchorX, anchorY)
        val p2 = android.graphics.PointF(anchorX, anchorY + 40f)
        val p3 = android.graphics.PointF(anchorX - 50f, anchorY + 80f)
        val p4 = android.graphics.PointF(p3.x - 30f, p3.y)
        
        val path = android.graphics.Path()
        path.moveTo(p1.x, p1.y)
        path.lineTo(p2.x, p2.y)
        path.lineTo(p3.x, p3.y)
        path.lineTo(p4.x, p4.y)
        canvas.drawPath(path, linePaint)
        
        canvas.drawCircle(p1.x, p1.y, 5f, nodePaint)
        canvas.drawCircle(p3.x, p3.y, 6f, nodePaint)
        
        val boxLeft = p4.x - boxWidth
        val boxTop = p4.y - boxHeight / 2
        
        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(45, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, bgPaint)
        
        val accentBarPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = android.graphics.Paint.Style.FILL
        }
        val barX = boxLeft + boxWidth - 6f
        canvas.drawRect(barX, boxTop, barX + 6f, boxTop + boxHeight, accentBarPaint)
        canvas.drawRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, linePaint)
        canvas.drawText(message, boxLeft + padding, boxTop + boxHeight / 2 + 10f, paintText)

        ivHudHintHeader.setImageBitmap(bitmap)
        ivHudHintHeader.visibility = android.view.View.VISIBLE
        ivHudHintHeader.alpha = 0f
        ivHudHintHeader.translationY = -20f
        
        ivHudHintHeader.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .withEndAction {
                ivHudHintHeader.animate()
                    .alpha(0f)
                    .translationY(80f)
                    .setStartDelay(1500)
                    .setDuration(500)
                    .withEndAction {
                        ivHudHintHeader.visibility = android.view.View.GONE
                    }
                    .start()
            }
            .start()
    }

    fun setBottomNavVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        bottomNav.visibility = visibility
        btnHeaderBack.visibility = if (visible) View.GONE else View.VISIBLE
    }

    fun loadFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.nav_host_fragment, fragment, tag)
            .commit()
    }

    fun openAddProperty(property: PropertyDTO?, sharedImage: View? = null, sharedTitle: View? = null) {
        updateTitle(if (property != null) "Edit Property" else "Property Addition")
        val fragment = AddPropertyFragment()
        val bundle = Bundle()
        bundle.putSerializable("property", property)
        
        if (sharedImage != null) bundle.putString("TRANSITION_IMAGE_NAME", sharedImage.transitionName)
        if (sharedTitle != null) bundle.putString("TRANSITION_TITLE_NAME", sharedTitle.transitionName)
        
        fragment.arguments = bundle
        
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        
        if (sharedImage != null && sharedTitle != null) {
            transaction.addSharedElement(sharedImage, sharedImage.transitionName)
            transaction.addSharedElement(sharedTitle, sharedTitle.transitionName)
        }
        
        transaction.replace(R.id.nav_host_fragment, fragment, "add_property")
            .addToBackStack(null)
            .commit()
    }

    fun openPropertyExplore(property: PropertyDTO, sharedElements: Map<String, View>? = null) {
        val fragment = PropertyExploreFragment()
        val bundle = Bundle()
        bundle.putSerializable("property", property)
        
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        
        sharedElements?.forEach { (name, view) ->
            transaction.addSharedElement(view, name)
            bundle.putString("TRANSITION_${name.substringBeforeLast("_").uppercase()}_NAME", name)
        }
        
        fragment.arguments = bundle
        
        transaction.replace(R.id.nav_host_fragment, fragment, "property_explore")
            .addToBackStack(null)
            .commit()
    }

    fun openChat(property: PropertyDTO) {
        val fragment = ChatFragment()
        val bundle = Bundle()
        bundle.putSerializable("property", property)
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment, "chat")
            .addToBackStack(null)
            .commit()
    }

    private fun logout() {
        sessionManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
