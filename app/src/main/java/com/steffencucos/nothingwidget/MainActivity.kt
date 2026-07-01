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
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.steffencucos.nothingwidget.location.DeviceLocationProvider
import com.steffencucos.nothingwidget.location.LocationStore
import com.steffencucos.nothingwidget.solar.SolarEventRepository
import com.steffencucos.nothingwidget.widget.ColorWheelView
import com.steffencucos.nothingwidget.widget.PhaseWatchIconRenderer
import com.steffencucos.nothingwidget.widget.SolarEventWidgetProvider
import com.steffencucos.nothingwidget.widget.TimeDotMatrixRenderer
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
    private var lastActualWidgetRefreshMs = 0L

    private lateinit var statusText: TextView
    private lateinit var actionButton: Button
    private lateinit var styleClassicButton: Button
    private lateinit var styleNothingButton: Button
    private lateinit var accentColorLabel: TextView
    private lateinit var customAccentLabel: TextView
    private lateinit var colorWheelView: ColorWheelView
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
        if (permissions.values.any { it }) refreshLocation() else renderPermissionNeeded()
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
        refreshActualWidget(force = true)
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
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(48), dp(32), dp(48))
            setBackgroundColor(0xFF111111.toInt())
            addView(title)
            addView(previewContainer)
            addView(configureButton)
            addView(hintText)
        })
        rebuildPreviewIfNeeded(force = true)
        startLivePreview()
        refreshActualWidget(force = true)
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

    private fun buildConfigurationView(): View {
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
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
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
                })
            }
        }
        customAccentLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFBDBDBD.toInt())
        }
        colorWheelView = ColorWheelView(this).apply {
            setSelectedColor(WidgetPreferences.getAccentColorArgb(this@MainActivity))
            onColorChanged = { color -> setCustomAccentColor(color) }
            layoutParams = LinearLayout.LayoutParams(dp(220), dp(220)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(6)
                bottomMargin = dp(10)
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
                    if (fromUser) setDotTextSize(WidgetPreferences.MIN_DOT_TEXT_SIZE_SP + progress)
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
                if (WidgetPreferences.isTimeSimulationEnabled(this@MainActivity) != isChecked) setTimeSimulationEnabled(isChecked)
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
                    if (fromUser) setTimeSimulationMultiplier(WidgetPreferences.MIN_TIME_SIMULATION_MULTIPLIER + progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        timeSimulationSubtitle = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFFBDBDBD.toInt())
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
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
            addView(customAccentLabel)
            addView(colorWheelView)
            addView(dotSizeLabel)
            addView(dotSizeSlider)
            addView(timeSimulationSwitch)
            addView(timeSpeedLabel)
            addView(timeSpeedSlider)
            addView(timeSimulationSubtitle)
        }

        return ScrollView(this).apply {
            setBackgroundColor(0xFF111111.toInt())
            addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
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
        val accentColor = WidgetPreferences.getAccentColorArgb(this)
        val dark = isDarkMode()
        val iconDp = if (style == WidgetStyle.NOTHING) 54 else 40
        val iconBitmap = PhaseWatchIconRenderer.render(dp(iconDp), phaseFor(event.label, event.progressPercent), dark, accentColor)
        view.findViewById<TextView>(R.id.eventStatus)?.text = event.statusText.uppercase()
        view.findViewById<ImageView>(R.id.eventIcon)?.setImageBitmap(iconBitmap)
        if (style == WidgetStyle.NOTHING) {
            val timeColor = if (dark) 0xFFFFFFFF.toInt() else 0xFF111111.toInt()
            view.findViewById<TextView>(R.id.eventLabel)?.apply {
                text = event.label.uppercase()
                textSize = 8f
            }
            view.findViewById<ImageView>(R.id.eventTime)?.setImageBitmap(
                TimeDotMatrixRenderer.render(event.displayTime, dp(20), timeColor)
            )
            view.findViewById<TextView>(R.id.eventRemaining)?.text = remainingText(event.timeRemaining)
            applyAccentColorToPreview(view, accentColor)
        } else {
            view.findViewById<TextView>(R.id.eventLabel)?.text = event.label.uppercase()
            view.findViewById<TextView>(R.id.eventTime)?.text = event.displayTime.uppercase()
            view.findViewById<TextView>(R.id.eventRemaining)?.text = event.timeRemaining
        }
        view.findViewById<TextView>(R.id.progressText)?.text = "${event.progressPercent}%"
        view.findViewById<ProgressBar>(R.id.eventProgress)?.progress = event.progressPercent
        refreshActualWidget()
    }

    private fun refreshActualWidget(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastActualWidgetRefreshMs < ACTUAL_WIDGET_REFRESH_INTERVAL_MS) return
        lastActualWidgetRefreshMs = now
        SolarEventWidgetProvider.refreshAll(this)
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

    private fun remainingText(value: String): String = "IN ${value.uppercase().replace(" LEFT", "").trim()}"

    private fun renderCurrentState() {
        if (!locationProvider.hasLocationPermission()) {
            renderPermissionNeeded()
            return
        }
        if (locationStore.get() == null) refreshLocation() else renderReady()
    }

    private fun requestOrRefreshLocation() {
        if (!locationProvider.hasLocationPermission()) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
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
                refreshActualWidget(force = true)
                renderReady()
            }
        }
    }

    private fun setWidgetStyle(style: WidgetStyle) {
        WidgetPreferences.setStyle(this, style)
        renderStyleState()
        refreshActualWidget(force = true)
    }

    private fun setAccentColor(accentColor: WidgetAccentColor) {
        WidgetPreferences.setAccentColor(this, accentColor)
        renderAccentColorState()
        refreshActualWidget(force = true)
    }

    private fun setCustomAccentColor(color: Int) {
        WidgetPreferences.setCustomAccentColor(this, color)
        renderAccentColorState()
        refreshActualWidget(force = true)
    }

    private fun setDotTextSize(sizeSp: Int) {
        WidgetPreferences.setDotTextSizeSp(this, sizeSp)
        renderDotSizeState()
        refreshActualWidget(force = true)
    }

    private fun setTimeSimulationEnabled(enabled: Boolean) {
        WidgetPreferences.setTimeSimulationEnabled(this, enabled)
        renderTimeSimulationState()
        refreshActualWidget(force = true)
    }

    private fun setTimeSimulationMultiplier(multiplier: Int) {
        WidgetPreferences.setTimeSimulationMultiplier(this, multiplier)
        renderTimeSimulationState()
        refreshActualWidget(force = true)
    }

    private fun renderStyleState() {
        val currentStyle = WidgetPreferences.getStyle(this)
        styleClassicButton.text = styleLabel(getString(R.string.widget_style_classic), currentStyle == WidgetStyle.CLASSIC)
        styleNothingButton.text = styleLabel(getString(R.string.widget_style_nothing), currentStyle == WidgetStyle.NOTHING)
    }

    private fun renderAccentColorState() {
        val currentAccentColor = WidgetPreferences.getAccentColor(this)
        val currentAccentArgb = WidgetPreferences.getAccentColorArgb(this)
        val usingCustomAccent = WidgetPreferences.isCustomAccentColor(this)
        accentColorLabel.text = "Accent color: ${WidgetPreferences.getAccentColorDisplayName(this)}"
        accentColorButtons.forEach { (accentColor, button) ->
            button.text = if (!usingCustomAccent && accentColor == currentAccentColor) "✓ ${accentColor.displayName}" else accentColor.displayName
        }
        customAccentLabel.text = "${if (usingCustomAccent) "✓ " else ""}Colour wheel: ${formatHexColor(currentAccentArgb)}"
        colorWheelView.setSelectedColor(currentAccentArgb)
    }

    private fun renderDotSizeState() {
        val sizeSp = WidgetPreferences.getDotTextSizeSp(this)
        dotSizeLabel.text = "${getString(R.string.dot_size_title)}: ${sizeSp}sp"
        val targetProgress = sizeSp - WidgetPreferences.MIN_DOT_TEXT_SIZE_SP
        if (dotSizeSlider.progress != targetProgress) dotSizeSlider.progress = targetProgress
    }

    private fun renderTimeSimulationState() {
        val enabled = WidgetPreferences.isTimeSimulationEnabled(this)
        val multiplier = WidgetPreferences.getTimeSimulationMultiplier(this)
        if (timeSimulationSwitch.isChecked != enabled) timeSimulationSwitch.isChecked = enabled
        timeSpeedLabel.text = "Time speed: ${multiplier}×"
        val targetProgress = multiplier - WidgetPreferences.MIN_TIME_SIMULATION_MULTIPLIER
        if (timeSpeedSlider.progress != targetProgress) timeSpeedSlider.progress = targetProgress
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

    private fun styleLabel(label: String, selected: Boolean): String = if (selected) "✓ $label" else label

    private fun textColorForBackground(color: Int): Int {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
        return if (luminance > 150.0) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
    }

    private fun formatHexColor(color: Int): String = "#%06X".format(color and 0x00FFFFFF)

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

    companion object {
        private const val ACTUAL_WIDGET_REFRESH_INTERVAL_MS = 5_000L
    }
}
