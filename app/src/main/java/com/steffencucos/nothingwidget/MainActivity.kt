package com.steffencucos.nothingwidget

import android.Manifest
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.steffencucos.nothingwidget.location.DeviceLocationProvider
import com.steffencucos.nothingwidget.location.LocationStore
import com.steffencucos.nothingwidget.solar.SolarEventRepository
import com.steffencucos.nothingwidget.widget.DotMatrixText
import com.steffencucos.nothingwidget.widget.PhaseWatchIconRenderer
import com.steffencucos.nothingwidget.widget.SolarEventWidgetProvider
import com.steffencucos.nothingwidget.widget.WidgetAccentColor
import com.steffencucos.nothingwidget.widget.WidgetPreferences
import com.steffencucos.nothingwidget.widget.WidgetStyle

class MainActivity : AppCompatActivity() {
    private lateinit var locationProvider: DeviceLocationProvider
    private lateinit var locationStore: LocationStore
    private lateinit var solarEventRepository: SolarEventRepository

    private var previewContainer: FrameLayout? = null
    private var previewView: View? = null
    private var previewLayoutId: Int? = null

    private lateinit var statusText: TextView
    private lateinit var actionButton: Button
    private lateinit var styleClassicButton: Button
    private lateinit var styleNothingButton: Button
    private lateinit var accentColorLabel: TextView
    private val accentColorButtons = mutableMapOf<WidgetAccentColor, Button>()
    private lateinit var dotSizeLabel: TextView
    private lateinit var dotSizeSlider: SeekBar
    private lateinit var timeSimulationSwitch: Switch
    private lateinit var timeSpeedLabel: TextView
    private lateinit var timeSpeedSlider: SeekBar
    private lateinit var timeSimulationSubtitle: TextView

    private val previewHandler = Handler(Looper.getMainLooper())
    private val previewTicker = object : Runnable {
        override fun run() {
            updateLivePreview()
            previewHandler.postDelayed(this, 1_000L)
        }
    }

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
        solarEventRepository = SolarEventRepository(this)
        showPreviewScreen()
    }

    override fun onResume() {
        super.onResume()
        startLivePreview()
    }

    override fun onPause() {
        stopLivePreview()
        super.onPause()
    }

    private fun showPreviewScreen() {
        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        previewContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(160), dp(160)).apply {
                topMargin = dp(32)
                bottomMargin = dp(32)
            }
        }
        previewLayoutId = null
        previewView = null

        val configureButton = Button(this).apply {
            text = "Configure"
            setOnClickListener { showConfigurationScreen() }
        }

        val hintText = TextView(this).apply {
            text = "Live 2×2 widget preview"
            textSize = 12f
            setTextColor(0xFFBDBDBD.toInt())
            gravity = Gravity.CENTER
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(48), dp(32), dp(48))
            setBackgroundColor(0xFF111111.toInt())
            addView(title)
            addView(previewContainer)
            addView(configureButton)
            addView(hintText)
        }

        setContentView(root)
        rebuildPreviewIfNeeded(force = true)
        startLivePreview()
    }

    private fun showConfigurationScreen() {
        stopLivePreview()
        setContentView(buildConfigurationView())
        renderCurrentState()
        renderStyleState()
        renderAccentColorState()
        renderDotSizeState()
        renderTimeSimulationState()
    }

    private fun buildConfigurationView(): LinearLayout {
        accentColorButtons.clear()

        val backButton = Button(this).apply {
            text = "Back to preview"
            setOnClickListener { showPreviewScreen() }
        }

        val title = TextView(this).apply {
            text = "Configuration"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
        }

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
            addView(styleClassicButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(styleNothingButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        accentColorLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFBDBDBD.toInt())
        }

        val accentColorGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            WidgetAccentColor.entries.chunked(4).forEach { rowColors ->
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    rowColors.forEach { accentColor ->
                        val button = Button(this@MainActivity).apply {
                            text = accentColor.displayName
                            setTextColor(textColorForBackground(accentColor.argb))
                            setBackgroundColor(accentColor.argb)
                            setOnClickListener { setAccentColor(accentColor) }
                        }
                        accentColorButtons[accentColor] = button
                        addView(button, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    }
                }
                addView(row)
            }
        }

        dotSizeLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFBDBDBD.toInt())
        }

        dotSizeSlider = SeekBar(this).apply {
            max = WidgetPreferences.MAX_DOT_TEXT_SIZE_SP - WidgetPreferences.MIN_DOT_TEXT_SIZE_SP
            progress = WidgetPreferences.getDotTextSizeSp(this@MainActivity) - WidgetPreferences.MIN_DOT_TEXT_SIZE_SP
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    setDotTextSize(WidgetPreferences.MIN_DOT_TEXT_SIZE_SP + progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        timeSimulationSwitch = Switch(this).apply {
            text = getString(R.string.time_simulation_title)
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (WidgetPreferences.isTimeSimulationEnabled(this@MainActivity) != isChecked) {
                    setTimeSimulationEnabled(isChecked)
                }
            }
        }

        timeSpeedLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFBDBDBD.toInt())
        }

        timeSpeedSlider = SeekBar(this).apply {
            max = WidgetPreferences.MAX_TIME_SIMULATION_MULTIPLIER - WidgetPreferences.MIN_TIME_SIMULATION_MULTIPLIER
            progress = WidgetPreferences.getTimeSimulationMultiplier(this@MainActivity) - WidgetPreferences.MIN_TIME_SIMULATION_MULTIPLIER
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    setTimeSimulationMultiplier(WidgetPreferences.MIN_TIME_SIMULATION_MULTIPLIER + progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }

        timeSimulationSubtitle = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFFBDBDBD.toInt())
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(32), dp(48), dp(32), dp(48))
            setBackgroundColor(0xFF111111.toInt())
            addView(backButton)
            addView(title)
            addView(statusText)
            addView(actionButton)
            addView(styleTitle)
            addView(styleRow)
            addView(accentColorLabel)
            addView(accentColorGrid)
            addView(dotSizeLabel)
            addView(dotSizeSlider)
            addView(timeSimulationSwitch)
            addView(timeSpeedLabel)
            addView(timeSpeedSlider)
            addView(timeSimulationSubtitle)
        }
    }

    private fun startLivePreview() {
        previewHandler.removeCallbacks(previewTicker)
        previewHandler.post(previewTicker)
    }

    private fun stopLivePreview() {
        previewHandler.removeCallbacks(previewTicker)
    }

    private fun updateLivePreview() {
        rebuildPreviewIfNeeded()
        val view = previewView ?: return
        val event = solarEventRepository.getNextEvent(WidgetPreferences.currentWidgetTime(this))
        val style = WidgetPreferences.getStyle(this)
        val accentColor = WidgetPreferences.getAccentColor(this).argb
        val iconSizeDp = if (style == WidgetStyle.NOTHING) 34 else 40
        val iconBitmap = PhaseWatchIconRenderer.render(
            sizePx = dp(iconSizeDp),
            phase = phaseFor(event.label, event.progressPercent),
            darkMode = isDarkMode(),
            accentColor = accentColor
        )

        view.findViewById<TextView>(R.id.eventStatus)?.text = event.statusText.uppercase()
        view.findViewById<ImageView>(R.id.eventIcon)?.setImageBitmap(iconBitmap)
        if (style == WidgetStyle.NOTHING) {
            val dotTextSizeSp = WidgetPreferences.getDotTextSizeSp(this).toFloat()
            view.findViewById<TextView>(R.id.eventLabel)?.apply {
                text = DotMatrixText.render(event.label, maxCharacters = 7)
                textSize = dotTextSizeSp
            }
            view.findViewById<TextView>(R.id.eventTime)?.apply {
                text = DotMatrixText.render(event.displayTime, maxCharacters = 7)
                textSize = dotTextSizeSp
            }
            applyAccentColorToPreview(view, accentColor)
        } else {
            view.findViewById<TextView>(R.id.eventLabel)?.text = event.label.uppercase()
            view.findViewById<TextView>(R.id.eventTime)?.text = event.displayTime.uppercase()
        }
        view.findViewById<TextView>(R.id.eventRemaining)?.text = event.timeRemaining
        view.findViewById<TextView>(R.id.progressText)?.text = "${event.progressPercent}%"
        view.findViewById<ProgressBar>(R.id.eventProgress)?.progress = event.progressPercent
    }

    private fun applyAccentColorToPreview(view: View, accentColor: Int) {
        view.findViewById<TextView>(R.id.statusAccentDot)?.setTextColor(accentColor)
        view.findViewById<TextView>(R.id.iconAccentDot)?.setTextColor(accentColor)
        view.findViewById<TextView>(R.id.progressText)?.setTextColor(accentColor)
        view.findViewById<ProgressBar>(R.id.eventProgress)?.progressTintList = ColorStateList.valueOf(accentColor)
    }

    private fun rebuildPreviewIfNeeded(force: Boolean = false) {
        val container = previewContainer ?: return
        val nextLayoutId = widgetLayout()
        if (!force && previewLayoutId == nextLayoutId && previewView != null) return

        container.removeAllViews()
        previewView = LayoutInflater.from(this).inflate(nextLayoutId, container, false).also { view ->
            container.addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        previewLayoutId = nextLayoutId
    }

    private fun widgetLayout(): Int {
        val style = WidgetPreferences.getStyle(this)
        if (style == WidgetStyle.CLASSIC) return R.layout.widget_solar_event
        return if (isDarkMode()) R.layout.widget_solar_event_nothing else R.layout.widget_solar_event_nothing_light
    }

    private fun phaseFor(label: String, progressPercent: Int): Float {
        val progress = progressPercent.coerceIn(0, 100) / 100f
        return if (label.uppercase() == "SUNSET") progress * 0.5f else 0.5f + progress * 0.5f
    }

    private fun isDarkMode(): Boolean {
        val mode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES
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

    private fun setAccentColor(accentColor: WidgetAccentColor) {
        WidgetPreferences.setAccentColor(this, accentColor)
        renderAccentColorState()
        SolarEventWidgetProvider.refreshAll(this)
    }

    private fun setDotTextSize(sizeSp: Int) {
        WidgetPreferences.setDotTextSizeSp(this, sizeSp)
        renderDotSizeState()
        SolarEventWidgetProvider.refreshAll(this)
    }

    private fun setTimeSimulationEnabled(enabled: Boolean) {
        WidgetPreferences.setTimeSimulationEnabled(this, enabled)
        renderTimeSimulationState()
        SolarEventWidgetProvider.refreshAll(this)
    }

    private fun setTimeSimulationMultiplier(multiplier: Int) {
        WidgetPreferences.setTimeSimulationMultiplier(this, multiplier)
        renderTimeSimulationState()
        SolarEventWidgetProvider.refreshAll(this)
    }

    private fun renderStyleState() {
        val currentStyle = WidgetPreferences.getStyle(this)
        styleClassicButton.text = styleLabel(getString(R.string.widget_style_classic), currentStyle == WidgetStyle.CLASSIC)
        styleNothingButton.text = styleLabel(getString(R.string.widget_style_nothing), currentStyle == WidgetStyle.NOTHING)
    }

    private fun renderAccentColorState() {
        val currentAccentColor = WidgetPreferences.getAccentColor(this)
        accentColorLabel.text = "Accent color: ${currentAccentColor.displayName}"
        accentColorButtons.forEach { (accentColor, button) ->
            button.text = if (accentColor == currentAccentColor) "✓ ${accentColor.displayName}" else accentColor.displayName
        }
    }

    private fun renderDotSizeState() {
        val sizeSp = WidgetPreferences.getDotTextSizeSp(this)
        dotSizeLabel.text = "${getString(R.string.dot_size_title)}: ${sizeSp}sp"
        val targetProgress = sizeSp - WidgetPreferences.MIN_DOT_TEXT_SIZE_SP
        if (dotSizeSlider.progress != targetProgress) {
            dotSizeSlider.progress = targetProgress
        }
    }

    private fun renderTimeSimulationState() {
        val enabled = WidgetPreferences.isTimeSimulationEnabled(this)
        val multiplier = WidgetPreferences.getTimeSimulationMultiplier(this)
        if (timeSimulationSwitch.isChecked != enabled) {
            timeSimulationSwitch.isChecked = enabled
        }
        timeSpeedLabel.text = "Time speed: ${multiplier}×"
        val targetProgress = multiplier - WidgetPreferences.MIN_TIME_SIMULATION_MULTIPLIER
        if (timeSpeedSlider.progress != targetProgress) {
            timeSpeedSlider.progress = targetProgress
        }
        timeSimulationSubtitle.text = "Full solar day in about ${formatSimulationDayLength(multiplier)}"
    }

    private fun formatSimulationDayLength(multiplier: Int): String {
        val minutes = 1440.0 / multiplier.toDouble()
        return if (minutes >= 60.0) {
            val hours = (minutes / 60.0).toInt()
            val remainingMinutes = (minutes - hours * 60).toInt()
            if (remainingMinutes == 0) "${hours}h" else "${hours}h ${remainingMinutes}m"
        } else {
            "${kotlin.math.round(minutes).toInt()}m"
        }
    }

    private fun styleLabel(label: String, selected: Boolean): String =
        if (selected) "✓ $label" else label

    private fun textColorForBackground(color: Int): Int {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
        return if (luminance > 150.0) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
    }

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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
