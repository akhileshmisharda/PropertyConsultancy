package com.example.propertyconsultancy.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import com.example.propertyconsultancy.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val mPath = Path()
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Radius of the hump
    private var curveCircleRadius = 0f
    // Offset for the hump (how much it sticks out above the bar)
    private var curveOffset = 0f

    init {
        val density = resources.displayMetrics.density
        curveCircleRadius = 56f * density 
        curveOffset = 22f * density   // Moved border up from 30dp to 22dp

        mPaint.apply {
            style = Paint.Style.FILL
            color = Color.TRANSPARENT // Fully transparent background
        }

        mBorderPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            color = ContextCompat.getColor(context, R.color.success_green)
            strokeCap = Paint.Cap.ROUND
        }

        setBackgroundColor(Color.TRANSPARENT)
        // Ensure onDraw is called
        setWillNotDraw(false)
        
        // Disable clipping recursively
        clipChildren = false
        clipToPadding = false
    }

    private fun disableClipping(view: View) {
        if (view is ViewGroup) {
            view.clipChildren = false
            view.clipToPadding = false
            for (i in 0 until view.childCount) {
                disableClipping(view.getChildAt(i))
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val width = w.toFloat()
        val height = h.toFloat()
        val centerX = width / 2f
        
        val top = curveOffset
        
        mPath.reset()
        mPath.moveTo(0f, top)
        
        // Line to the start of the curve
        mPath.lineTo(centerX - curveCircleRadius, top)
        
        // Convex Curve (The Hump)
        mPath.cubicTo(
            centerX - (curveCircleRadius * 0.5f), top,
            centerX - (curveCircleRadius * 0.5f), 0f,
            centerX, 0f
        )
        mPath.cubicTo(
            centerX + (curveCircleRadius * 0.5f), 0f,
            centerX + (curveCircleRadius * 0.5f), top,
            centerX + curveCircleRadius, top
        )
        
        mPath.lineTo(width, top)
        mPath.lineTo(width, height)
        mPath.lineTo(0f, height)
        mPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the filled background
        canvas.drawPath(mPath, mPaint)

        // Draw the border path (top edge only)
        val borderPath = Path()
        val centerX = width / 2f
        val top = curveOffset
        
        borderPath.moveTo(0f, top)
        borderPath.lineTo(centerX - curveCircleRadius, top)
        borderPath.cubicTo(
            centerX - (curveCircleRadius * 0.5f), top,
            centerX - (curveCircleRadius * 0.5f), 0f,
            centerX, 0f
        )
        borderPath.cubicTo(
            centerX + (curveCircleRadius * 0.5f), 0f,
            centerX + (curveCircleRadius * 0.5f), top,
            centerX + curveCircleRadius, top
        )
        borderPath.lineTo(width.toFloat(), top)
        
        canvas.drawPath(borderPath, mBorderPaint)
        
        super.onDraw(canvas)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        
        val menuView = getChildAt(0) as? ViewGroup ?: return
        disableClipping(menuView)
        
        val density = resources.displayMetrics.density

        menuView.post {
            for (i in 0 until menuView.childCount) {
                val item = menuView.getChildAt(i) as? ViewGroup ?: continue
                
                val iconContainer = item.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_container)
                val labelGroup = item.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_labels_group)

                if (i == 2) {
                    // Dashboard - Pushed down to avoid touching the border peak
                    labelGroup?.visibility = View.GONE
                    if (iconContainer != null) {
                        iconContainer.scaleX = 1.6f
                        iconContainer.scaleY = 1.6f
                        // Pushed down from 10dp to 18dp
                        iconContainer.translationY = 18f * density
                    }
                } else {
                    // Others - Pushed down by 8dp to avoid the border and maintaining tight gap
                    if (iconContainer != null) {
                        // Pushed down from 10dp to 18dp
                        iconContainer.translationY = 18f * density 
                    }
                    if (labelGroup != null) {
                        // Moved down with the icon to maintain the tight gap (from -8dp to 0dp)
                        labelGroup.translationY = 0f
                    }
                }
            }
        }
    }
}
