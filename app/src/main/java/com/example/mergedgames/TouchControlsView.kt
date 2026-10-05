package com.example.mergedgames

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import org.libsdl.app.SDLActivity
import kotlin.math.hypot
import kotlin.math.min

/**
 * On-screen gamepad drawn over the SDL surface. It feeds Android key codes into SDL, which maps
 * them to the keys the Lux Engine's default controller expects (platform_controls.cpp):
 *   buttons 1-4 = A S D Q, shoulders = W E, start = Enter, back/cancel = Escape, move = arrow keys.
 * Touches that land outside every control fall through to the game (SDL turns them into pointer input).
 */
class TouchControlsView(context: Context) : View(context) {

    private class Btn(val label: String, val keyCode: Int) {
        var cx = 0f
        var cy = 0f
        var r = 0f
    }

    private val face = listOf(
        Btn("A", KeyEvent.KEYCODE_A),  // bottom
        Btn("S", KeyEvent.KEYCODE_S),  // right
        Btn("D", KeyEvent.KEYCODE_D),  // top
        Btn("Q", KeyEvent.KEYCODE_Q),  // left
    )
    private val shoulderL = Btn("W", KeyEvent.KEYCODE_W)
    private val shoulderR = Btn("E", KeyEvent.KEYCODE_E)
    private val start = Btn("START", KeyEvent.KEYCODE_ENTER)
    private val back = Btn("BACK", KeyEvent.KEYCODE_ESCAPE)
    private val buttons = face + listOf(shoulderL, shoulderR, start, back)

    private var dpadCx = 0f
    private var dpadCy = 0f
    private var dpadR = 0f

    private val down = HashSet<Int>()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(150, 255, 255, 255)
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val u = min(w, h) / 100f  // 1 unit = 1% of the short side

        dpadR = 24f * u
        dpadCx = 5f * u + dpadR
        dpadCy = h - 5f * u - dpadR

        val fcx = w - 5f * u - 24f * u
        val fcy = h - 5f * u - 24f * u
        val off = 15f * u
        face[0].apply { cx = fcx;       cy = fcy + off; r = 10f * u }
        face[1].apply { cx = fcx + off; cy = fcy;       r = 10f * u }
        face[2].apply { cx = fcx;       cy = fcy - off; r = 10f * u }
        face[3].apply { cx = fcx - off; cy = fcy;       r = 10f * u }

        shoulderL.apply { cx = 14f * u;     cy = 14f * u; r = 8f * u }
        shoulderR.apply { cx = w - 14f * u; cy = 14f * u; r = 8f * u }
        start.apply { cx = w / 2f + 10f * u; cy = h - 8f * u; r = 6f * u }
        back.apply  { cx = w / 2f - 10f * u; cy = h - 8f * u; r = 6f * u }
    }

    override fun onDraw(canvas: Canvas) {
        // D-pad
        fill.color = Color.argb(60, 255, 255, 255)
        canvas.drawCircle(dpadCx, dpadCy, dpadR, fill)
        canvas.drawCircle(dpadCx, dpadCy, dpadR, ring)
        text.textSize = dpadR * 0.45f
        val t = dpadR * 0.62f
        arrow(canvas, "▲", KeyEvent.KEYCODE_DPAD_UP, dpadCx, dpadCy - t)
        arrow(canvas, "▼", KeyEvent.KEYCODE_DPAD_DOWN, dpadCx, dpadCy + t)
        arrow(canvas, "◀", KeyEvent.KEYCODE_DPAD_LEFT, dpadCx - t, dpadCy)
        arrow(canvas, "▶", KeyEvent.KEYCODE_DPAD_RIGHT, dpadCx + t, dpadCy)

        // Buttons
        for (b in buttons) {
            val pressed = b.keyCode in down
            fill.color = if (pressed) Color.argb(170, 255, 255, 255) else Color.argb(60, 255, 255, 255)
            canvas.drawCircle(b.cx, b.cy, b.r, fill)
            canvas.drawCircle(b.cx, b.cy, b.r, ring)
            text.textSize = if (b.label.length > 1) b.r * 0.45f else b.r * 0.9f
            text.color = if (pressed) Color.BLACK else Color.WHITE
            canvas.drawText(b.label, b.cx, b.cy + text.textSize * 0.35f, text)
        }
        text.color = Color.WHITE
    }

    private fun arrow(canvas: Canvas, glyph: String, key: Int, x: Float, y: Float) {
        text.color = if (key in down) Color.YELLOW else Color.WHITE
        canvas.drawText(glyph, x, y + text.textSize * 0.35f, text)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val action = e.actionMasked
        if (action == MotionEvent.ACTION_CANCEL) {
            applyKeys(emptySet())
            return true
        }

        val skip = if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) e.actionIndex else -1
        val want = HashSet<Int>()
        var hit = false
        for (i in 0 until e.pointerCount) {
            if (i == skip) continue
            if (hitTest(e.getX(i), e.getY(i), want)) hit = true
        }

        // Only claim the gesture if it started on a control; otherwise the game gets it.
        val consumed = hit || down.isNotEmpty()
        applyKeys(want)
        return consumed
    }

    private fun hitTest(x: Float, y: Float, want: MutableSet<Int>): Boolean {
        var hit = false

        val dx = x - dpadCx
        val dy = y - dpadCy
        if (hypot(dx, dy) <= dpadR * 1.25f) {
            hit = true
            val nx = dx / dpadR
            val ny = dy / dpadR
            if (nx < -0.3f) want.add(KeyEvent.KEYCODE_DPAD_LEFT)
            if (nx > 0.3f) want.add(KeyEvent.KEYCODE_DPAD_RIGHT)
            if (ny < -0.3f) want.add(KeyEvent.KEYCODE_DPAD_UP)
            if (ny > 0.3f) want.add(KeyEvent.KEYCODE_DPAD_DOWN)
        }

        for (b in buttons) {
            if (hypot(x - b.cx, y - b.cy) <= b.r * 1.3f) {
                hit = true
                want.add(b.keyCode)
            }
        }
        return hit
    }

    /** Sends key down/up to SDL for whatever changed since the last event. */
    private fun applyKeys(want: Set<Int>) {
        var changed = false
        for (k in want) {
            if (down.add(k)) {
                SDLActivity.onNativeKeyDown(k)
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                changed = true
            }
        }
        val released = down.filter { it !in want }
        for (k in released) {
            down.remove(k)
            SDLActivity.onNativeKeyUp(k)
            changed = true
        }
        if (changed) invalidate()
    }
}
