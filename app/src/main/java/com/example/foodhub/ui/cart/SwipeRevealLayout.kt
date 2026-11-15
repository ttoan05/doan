package com.example.foodhub.ui.cart

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs

class SwipeRevealLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mainView: View? = null
    private var menuView: View? = null
    private var actionWidth = 0

    var isOpen = false
        private set

    var onSwipeOpened: ((SwipeRevealLayout) -> Unit)? = null

    private lateinit var dragHelper: ViewDragHelper

    init {
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true

        dragHelper = ViewDragHelper.create(this, 1.0f, object : ViewDragHelper.Callback() {
            override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                return child == mainView
            }

            override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
                return left.coerceIn(-actionWidth, 0)
            }

            override fun getViewHorizontalDragRange(child: View): Int {
                return actionWidth
            }

            override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
                super.onViewPositionChanged(changedView, left, top, dx, dy)
                val mv = menuView ?: return
                val expectedMenuLeft = (mainView?.right ?: (width + left))
                val currentMenuLeft = mv.left
                val menuDx = expectedMenuLeft - currentMenuLeft
                if (menuDx != 0) {
                    mv.offsetLeftAndRight(menuDx)
                }
                postInvalidateOnAnimation()
            }

            override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
                val targetLeft = if (xvel < -600f) {
                    -actionWidth
                } else if (xvel > 600f) {
                    0
                } else if (releasedChild.left < -actionWidth / 2) {
                    -actionWidth
                } else {
                    0
                }

                isOpen = (targetLeft == -actionWidth)
                if (dragHelper.smoothSlideViewTo(releasedChild, targetLeft, 0)) {
                    postInvalidateOnAnimation()
                }
                if (isOpen) {
                    onSwipeOpened?.invoke(this@SwipeRevealLayout)
                }
            }
        })
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount >= 2) {
            mainView = getChildAt(0)
            menuView = getChildAt(1)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount < 2) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val main = getChildAt(0)
        val menu = getChildAt(1)
        mainView = main
        menuView = menu

        // 1. Measure main view using exact parent width
        main.measure(
            MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val measuredHeight = main.measuredHeight

        // 2. Measure menu view to match main view height exactly
        val menuHeightSpec = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        menu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), menuHeightSpec)
        actionWidth = menu.measuredWidth

        setMeasuredDimension(parentWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount < 2) {
            super.onLayout(changed, l, t, r, b)
            return
        }
        val main = getChildAt(0)
        val menu = getChildAt(1)
        mainView = main
        menuView = menu

        val w = measuredWidth
        val h = measuredHeight
        val offset = if (isOpen) -actionWidth else 0

        main.layout(offset, 0, offset + w, h)
        menu.layout(offset + w, 0, offset + w + actionWidth, h)
    }

    override fun computeScroll() {
        if (::dragHelper.isInitialized && dragHelper.continueSettling(true)) {
            postInvalidateOnAnimation()
        }
    }

    private var initialX = 0f
    private var initialY = 0f
    private var isDraggingHorizontally = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (childCount < 2 || !::dragHelper.isInitialized) return super.onInterceptTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                isDraggingHorizontally = false
                if (isOpen && ev.x < measuredWidth - actionWidth) {
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - initialX)
                val dy = abs(ev.y - initialY)
                if (dx > dragHelper.touchSlop && dx > dy * 1.2f) {
                    isDraggingHorizontally = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                } else if (dy > dragHelper.touchSlop && dy > dx) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingHorizontally = false
            }
        }
        return dragHelper.shouldInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (childCount < 2 || !::dragHelper.isInitialized) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                isDraggingHorizontally = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - initialX)
                val dy = abs(event.y - initialY)
                if (dx > dragHelper.touchSlop && dx > dy * 1.2f) {
                    isDraggingHorizontally = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isOpen && !isDraggingHorizontally && event.x < measuredWidth - actionWidth) {
                    close(smooth = true)
                    return true
                }
                isDraggingHorizontally = false
            }
            MotionEvent.ACTION_CANCEL -> {
                isDraggingHorizontally = false
            }
        }

        try {
            dragHelper.processTouchEvent(event)
        } catch (_: Exception) {
        }
        return true
    }

    fun open(smooth: Boolean = true) {
        if (childCount < 2 || !::dragHelper.isInitialized) return
        val main = mainView ?: getChildAt(0)
        val mv = menuView ?: getChildAt(1)
        isOpen = true
        if (actionWidth == 0 && menuView != null) {
            actionWidth = menuView!!.measuredWidth
        }
        if (actionWidth == 0) return
        if (smooth) {
            if (dragHelper.smoothSlideViewTo(main, -actionWidth, 0)) {
                postInvalidateOnAnimation()
            }
        } else {
            dragHelper.abort()
            main.offsetLeftAndRight(-actionWidth - main.left)
            val expectedMenuLeft = measuredWidth - actionWidth
            mv.offsetLeftAndRight(expectedMenuLeft - mv.left)
            requestLayout()
        }
    }

    fun close(smooth: Boolean = true) {
        if (childCount < 2 || !::dragHelper.isInitialized) return
        val main = mainView ?: getChildAt(0)
        val mv = menuView ?: getChildAt(1)
        isOpen = false
        if (smooth) {
            if (dragHelper.smoothSlideViewTo(main, 0, 0)) {
                postInvalidateOnAnimation()
            }
        } else {
            dragHelper.abort()
            main.offsetLeftAndRight(-main.left)
            val w = measuredWidth
            if (w > 0) {
                mv.offsetLeftAndRight(w - mv.left)
            }
            requestLayout()
        }
    }
}
