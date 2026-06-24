package com.steffencucos.nothingwidget

import android.Manifest
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.steffencucos.nothingwidget.location.DeviceLocationProvider
import com.steffencucos.nothingwidget.location.LocationStore
import com.steffencucos.nothingwidget.widget.SolarEventWidgetProvider
import com.steffencucos.nothingwidget.widget.WidgetPreferences
import com.steffencucos.nothingwidget.widget.WidgetStyle

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var actionButton: Button
    private lateinit var styleClassicButton: Button
    private lateinit var styleNothingButton: Button
    private lateinit var locationProvider: DeviceLocationProvider
    private lateinit var locationStore: LocationStore

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            refreshLocation()
        } else {
            renderPermissionNeeded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationProvider = DeviceLocationProvider(this)
        locationStore = LocationStore(this)
        setContentView(buildContentView())
        renderCurrentState()
        renderStyleState()
    }

    private fun buildContentView(): LinearLayout {
        statusText = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
        }

        actionButton = Button(this).apply {
            text = getString(R.string.enable_location_button)
            setOnClickListener { requestOrRefreshLocation() }
        }

        val styleTitle = TextView(this).apply {
            text = getString(R.string.widget_style_title)
            textSize = 14f
            setTextColor(0xFFBDBDBD.toInt())
        }

        styleClassicButton = Button(this).apply {
            text = getString(R.string.widget_style_classic)
            setOnClickListener { setWidgetStyle(WidgetStyle.CLASSIC) }
        }

        styleNothingButton = Button(this).apply {
            text = getString(R.string.widget_style_nothing)
            setOnClickListener { setWidgetStyle(WidgetStyle.NOTHING) }
        }

        val styleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                styleClassicButton,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(
                styleNothingButton,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(48, 72, 48, 48)
            setBackgroundColor(0xFF111111.toInt())
            addView(statusText)
            addView(actionButton)
            addView(styleTitle)
            addView(styleRow)
        }
    }

    private fun renderCurrentState() {
        if (!locationProvider.hasLocationPermission()) {
            renderPermissionNeeded()
            return
        }

        if (locationStore.get() == null) {
            refreshLocation()
        } else {
            renderReady()
        }
    }

    private fun requestOrRefreshLocation() {
        if (!locationProvider.hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            return
        }

        refreshLocation()
    }

    private fun refreshLocation() {
        statusText.text = getString(R.string.location_refreshing)
        actionButton.isEnabled = false

        locationProvider.getLastKnownLocation { location ->
            runOnUiThread {
                actionButton.isEnabled = true
                if (location == null) {
                    statusText.text = getString(R.string.location_unavailable)
                    actionButton.text = getString(R.string.retry_location_button)
                    return@runOnUiThread
                }

                locationStore.save(location)
                SolarEventWidgetProvider.refreshAll(this)
                renderReady()
            }
        }
    }

    private fun setWidgetStyle(style: WidgetStyle) {
        WidgetPreferences.setStyle(this, style)
        renderStyleState()
        SolarEventWidgetProvider.refreshAll(this)
    }

    private fun renderStyleState() {
        val currentStyle = WidgetPreferences.getStyle(this)
        styleClassicButton.text = styleLabel(
            getString(R.string.widget_style_classic),
            currentStyle == WidgetStyle.CLASSIC
        )
        styleNothingButton.text = styleLabel(
            getString(R.string.widget_style_nothing),
            currentStyle == WidgetStyle.NOTHING
        )
    }

    private fun styleLabel(label: String, selected: Boolean): String =
        if (selected) "✓ $label" else label

    private fun renderPermissionNeeded() {
        statusText.text = getString(R.string.permission_needed_message)
        actionButton.text = getString(R.string.enable_location_button)
        actionButton.isEnabled = true
    }

    private fun renderReady() {
        statusText.text = getString(R.string.ready_message)
        actionButton.text = getString(R.string.refresh_location_button)
        actionButton.isEnabled = true
    }
}
