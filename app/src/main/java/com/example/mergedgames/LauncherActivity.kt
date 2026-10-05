package com.example.mergedgames

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Home screen of the merged app: one entry per game, each running on its own engine. */
class LauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#14161C"))
            setPadding(48, 48, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "Merged Games"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })

        root.addView(gameButton("OpenZelda", ZeldaActivity::class.java))
        root.addView(gameButton("Minosoft", MinosoftActivity::class.java))

        setContentView(root)
    }

    private fun gameButton(label: String, target: Class<out Activity>) = Button(this).apply {
        text = label
        textSize = 20f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 32 }
        setOnClickListener { startActivity(Intent(this@LauncherActivity, target)) }
    }
}
