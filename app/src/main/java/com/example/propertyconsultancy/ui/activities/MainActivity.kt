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
                    updateTitle("Upgrade")
                    Toast.makeText(this, "Upgrade to Premium feature coming soon!", Toast.LENGTH_SHORT).show()
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

    fun setBottomNavVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        bottomNav.visibility = visibility
        btnHeaderBack.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
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
