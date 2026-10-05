package com.example.mergedgames

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

/**
 * Host screen for Minosoft. The Minecraft client core has not been ported to Android yet
 * (it depends on LWJGL/GLFW/JavaFX), so this screen only reports that. See PORTING.md,
 * phases A-D, for what replaces it.
 */
class MinosoftActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Minosoft is not ported to Android yet.\nSee PORTING.md (phases A-D)."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#14161C"))
            setPadding(48, 48, 48, 48)
        })
    }
}
