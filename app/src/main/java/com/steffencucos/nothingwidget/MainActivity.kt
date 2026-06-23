package com.steffencucos.nothingwidget

import android.Manifest
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = "NothingWidget\n\nAdd the sunrise/sunset widget from your launcher.\n\nLocation permission will be requested by the widget configuration flow in a future iteration."
            textSize = 18f
            setPadding(48, 72, 48, 48)
        }

        setContentView(textView)
    }
}
