package com.example.mapboxtest

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import android.view.MotionEvent

class NonScrollableRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        // Intercept touch events related to scrolling
        return when (e.action) {
            MotionEvent.ACTION_MOVE -> false
            else -> super.onInterceptTouchEvent(e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        // Disable touch events related to scrolling
        return when (e.action) {
            MotionEvent.ACTION_MOVE -> false
            else -> super.onTouchEvent(e)
        }
    }
}
