package com.vibe.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vibe.ui.R
import com.vibe.ui.components.VibeTabs
import com.vibe.ui.theme.VibeAnimations

/**
 * Side Menu Screen
 */
class SideMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vibe_screen_side_menu)

        val modeSwitch = findViewById<VibeTabs>(R.id.mode_switch)
        val menuChats = findViewById<LinearLayout>(R.id.menu_chats)
        val menuContacts = findViewById<LinearLayout>(R.id.menu_contacts)
        val menuCalls = findViewById<LinearLayout>(R.id.menu_calls)
        val menuBusiness = findViewById<LinearLayout>(R.id.menu_business)
        val menuMarketplace = findViewById<LinearLayout>(R.id.menu_marketplace)
        val menuStyles = findViewById<LinearLayout>(R.id.menu_styles)
        val menuSettings = findViewById<LinearLayout>(R.id.menu_settings)

        // Setup mode switch
        modeSwitch.setTabs(listOf("Личное", "Работа"))

        // Menu click handlers
        menuChats.setOnClickListener {
            finish()
        }

        menuContacts.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        menuCalls.setOnClickListener {
            startActivity(Intent(this, CallsActivity::class.java))
        }

        menuBusiness.setOnClickListener {
            startActivity(Intent(this, BusinessActivity::class.java))
        }

        menuMarketplace.setOnClickListener {
            startActivity(Intent(this, MarketplaceActivity::class.java))
        }

        menuStyles.setOnClickListener {
            startActivity(Intent(this, StylesActivity::class.java))
        }

        menuSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Animate entrance
        VibeAnimations.slideUp(findViewById(R.id.mode_switch), 300)
    }
}
