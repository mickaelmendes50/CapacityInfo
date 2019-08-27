package com.ph03nix_x.capacityinfo

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var pref: SharedPreferences
    private lateinit var darkMode: SwitchCompat
    private lateinit var fahrenheit: SwitchCompat
    private lateinit var settingsLayout: LinearLayout
    private lateinit var changeDesignCapacity: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        pref = getSharedPreferences("preferences", Context.MODE_PRIVATE)

        if(pref.getBoolean("dark_mode", false)) setTheme(R.style.DarkTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        darkMode = findViewById(R.id.dark_mode)
        fahrenheit = findViewById(R.id.temperature_in_fahrenheit)
        settingsLayout = findViewById(R.id.settings_layout)
        changeDesignCapacity = findViewById(R.id.change_design_capacity)

        if(pref.getBoolean("dark_mode", false)) {

            settingsLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.dark))

            darkMode.background = getDrawable(R.drawable.selecteditem)
            fahrenheit.background = getDrawable(R.drawable.selecteditem)
            changeDesignCapacity.background = getDrawable(R.drawable.selecteditem)

            changeDesignCapacity.setTextColor(Color.WHITE)
        }

        darkMode.isChecked = pref.getBoolean("dark_mode", false)
        fahrenheit.isChecked = pref.getBoolean("fahrenheit", false)

        darkMode.setOnCheckedChangeListener { _, b ->

            pref.edit().putBoolean("dark_mode", b).apply()
            MainActivity.instance!!.recreate()
            recreate()
        }

        fahrenheit.setOnCheckedChangeListener { _, b ->  pref.edit().putBoolean("fahrenheit", b).apply() }

        changeDesignCapacity.setOnClickListener {

            changeDesignCapacity()
        }
    }

    private fun changeDesignCapacity() {

        val dialog = AlertDialog.Builder(this)

        val view = LayoutInflater.from(this).inflate(R.layout.change_design_capacity, null)

        dialog.setView(view)

        val changeDesignCapacity = view.findViewById<EditText>(R.id.change_design_capacity_edit)

        changeDesignCapacity.setText(if(pref.getInt("design_capacity", 0) >= 0) pref.getInt("design_capacity", 0).toString()

        else (pref.getInt("design_capacity", 0) / -1).toString())

        dialog.setPositiveButton(getString(R.string.change)) { _, _ -> pref.edit().putInt("design_capacity", changeDesignCapacity.text.toString().toInt()).apply() }

        dialog.setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }

        dialog.show()
    }
}