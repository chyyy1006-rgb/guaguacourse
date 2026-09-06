package com.example.npucourse.overlay

import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.content.res.Configuration
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.npucourse.MainActivity
import com.example.npucourse.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class QuickOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: GlassOverlayView? = null
    private var params: WindowManager.LayoutParams? = null
    private var snapAnimator: ValueAnimator? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val openSettings = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_watermelon)
            .setContentTitle("快捷悬浮窗正在运行")
            .setContentText("双击或按设置的手势打开目标应用")
            .setContentIntent(openSettings)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val config = QuickOverlayPreferences.load(this)
        if (!config.enabled || config.temporarilyHidden || config.targetPackage.isBlank() || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        showOrRefresh(config)
        return START_STICKY
    }

    override fun onDestroy() {
        snapAnimator?.cancel()
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        params = null
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val p = params ?: return
        clamp(p)
        overlayView?.let { runCatching { windowManager.updateViewLayout(it, p) } }
        persistPosition()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOrRefresh(config: QuickOverlayConfig) {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        val size = dp(config.sizeDp)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val layout = WindowManager.LayoutParams(
            size,
            size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (config.x >= 0) config.x else screenWidth - size - dp(12)
            y = if (config.y >= 0) config.y else screenHeight / 3
        }
        params = layout
        clamp(layout)

        val icon = runCatching { packageManager.getApplicationIcon(config.targetPackage) }.getOrNull()
        val view = GlassOverlayView(this, icon, config) { event -> handleGesture(event, config) }
        overlayView = view
        windowManager.addView(view, layout)
    }

    private fun handleGesture(event: OverlayEvent, config: QuickOverlayConfig) {
        when (event) {
            is OverlayEvent.Move -> {
                val p = params ?: return
                p.x = event.x
                p.y = event.y
                clamp(p)
                overlayView?.let { windowManager.updateViewLayout(it, p) }
            }
            OverlayEvent.Release -> {
                if (config.snapToEdge) snapToNearestEdge()
                else persistPosition()
            }
            OverlayEvent.Trigger -> launchTarget(config.targetPackage)
        }
    }

    private fun launchTarget(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            overlayView?.showUnavailable()
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { startActivity(launchIntent) }.onFailure { overlayView?.showUnavailable() }
    }

    private fun snapToNearestEdge() {
        val p = params ?: return
        val view = overlayView ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val target = if (p.x + p.width / 2 < screenWidth / 2) dp(6) else screenWidth - p.width - dp(6)
        val start = p.x
        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260L
            interpolator = android.view.animation.OvershootInterpolator(0.72f)
            addUpdateListener {
                val fraction = it.animatedValue as Float
                p.x = (start + (target - start) * fraction).roundToInt()
                clamp(p)
                runCatching { windowManager.updateViewLayout(view, p) }
            }
            doOnEndCompat { persistPosition() }
            start()
        }
    }

    private fun clamp(p: WindowManager.LayoutParams) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val (topInset, bottomInset) = safeSystemInsets()
        p.x = p.x.coerceIn(0, (screenWidth - p.width).coerceAtLeast(0))
        p.y = p.y.coerceIn(topInset, (screenHeight - bottomInset - p.height).coerceAtLeast(topInset))
    }

    private fun persistPosition() {
        val p = params ?: return
        val current = QuickOverlayPreferences.load(this)
        QuickOverlayPreferences.save(this, current.copy(x = p.x, y = p.y))
    }

    private fun safeSystemInsets(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = windowManager.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            insets.top to insets.bottom
        } else {
            // Conservative pre-R fallback: overlay coordinates include both system bars.
            dp(24) to dp(48)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "快捷悬浮窗", NotificationManager.IMPORTANCE_LOW).apply {
                description = "显示快捷悬浮窗运行状态"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "quick_overlay"
        private const val NOTIFICATION_ID = 4602
        private const val ACTION_STOP = "com.example.npucourse.overlay.STOP"

        fun refresh(context: Context) {
            val config = QuickOverlayPreferences.load(context)
            if (!config.enabled || config.temporarilyHidden || config.targetPackage.isBlank() || !Settings.canDrawOverlays(context)) {
                stop(context)
                return
            }
            ContextCompat.startForegroundService(context, Intent(context, QuickOverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QuickOverlayService::class.java))
        }
    }
}

private sealed interface OverlayEvent {
    data class Move(val x: Int, val y: Int) : OverlayEvent
    data object Release : OverlayEvent
    data object Trigger : OverlayEvent
}

private enum class TouchState { Idle, Pressed, WaitingSecondTap, Dragging, GestureCandidate, Triggered, Cooldown }

@SuppressLint("ViewConstructor")
private class GlassOverlayView(
    context: Context,
    private val icon: Drawable?,
    private val config: QuickOverlayConfig,
    private val callback: (OverlayEvent) -> Unit
) : View(context) {
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()
    private val doubleTapDistance = touchSlop * 2.2f
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(190, 55, 72)
        textAlign = Paint.Align.CENTER
    }
    private val bodyRect = RectF()
    private val highlightRect = RectF()
    private var state = TouchState.Idle
    private var downRawX = 0f
    private var downRawY = 0f
    private var downWindowX = 0
    private var downWindowY = 0
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var secondTapCandidate = false
    private var cooldownUntil = 0L
    private var velocityTracker: VelocityTracker? = null
    private var unavailableUntil = 0L

    private val longPress = Runnable {
        if (state == TouchState.Pressed && !config.lockPosition) {
            state = TouchState.Dragging
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            animate().scaleX(1.06f).scaleY(1.06f).setDuration(100).start()
        }
    }

    init {
        alpha = config.opacity
        isClickable = true
        contentDescription = "${OverlayGesture.label(config.gesture)}打开${config.targetLabel}，长按拖动"
        elevation = resources.displayMetrics.density * 7f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = w * 0.055f
        bodyRect.set(inset, inset, w - inset, h - inset)
        highlightRect.set(inset * 2f, inset * 1.5f, w - inset * 2f, h * 0.63f)
        warningPaint.textSize = w * 0.48f
        backgroundPaint.shader = android.graphics.LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            Color.argb(220, 246, 252, 255),
            Color.argb(198, 181, 220, 255),
            android.graphics.Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = bodyRect.width() * 0.36f
        canvas.drawRoundRect(bodyRect, radius, radius, backgroundPaint)
        borderPaint.color = Color.argb(210, 255, 255, 255)
        borderPaint.strokeWidth = resources.displayMetrics.density * 1.15f
        canvas.drawRoundRect(bodyRect, radius, radius, borderPaint)
        highlightPaint.color = Color.argb(150, 255, 255, 255)
        highlightPaint.strokeWidth = resources.displayMetrics.density * 1.7f
        canvas.drawArc(highlightRect, 205f, 125f, false, highlightPaint)

        if (SystemClock.uptimeMillis() < unavailableUntil) {
            canvas.drawText("!", width / 2f, height * 0.68f, warningPaint)
        } else {
            icon?.let {
                val iconInset = (width * 0.19f).roundToInt()
                it.setBounds(iconInset, iconInset, width - iconInset, height - iconInset)
                it.draw(canvas)
            }
        }
    }

    fun showUnavailable() {
        unavailableUntil = SystemClock.uptimeMillis() + 1800L
        invalidate()
        handler.postDelayed(::invalidate, 1850L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val now = SystemClock.uptimeMillis()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (now < cooldownUntil) {
                    state = TouchState.Cooldown
                    return true
                }
                secondTapCandidate = lastTapTime > 0L && now - lastTapTime <= doubleTapTimeout &&
                    hypot(event.rawX - lastTapX, event.rawY - lastTapY) <= doubleTapDistance
                state = TouchState.Pressed
                downRawX = event.rawX
                downRawY = event.rawY
                val location = IntArray(2)
                getLocationOnScreen(location)
                downWindowX = location[0]
                downWindowY = location[1]
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                handler.postDelayed(longPress, longPressTimeout)
                animate().alpha(1f).scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                val distance = hypot(dx, dy)
                if (state == TouchState.Dragging) {
                    callback(OverlayEvent.Move((downWindowX + dx).roundToInt(), (downWindowY + dy).roundToInt()))
                } else if (distance > touchSlop) {
                    handler.removeCallbacks(longPress)
                    secondTapCandidate = false
                    state = TouchState.GestureCandidate
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                val distance = hypot(dx, dy)
                when (state) {
                    TouchState.Dragging -> callback(OverlayEvent.Release)
                    TouchState.GestureCandidate -> if (matchesSwipe(dx, dy, distance)) trigger()
                    TouchState.Pressed -> {
                        if (config.gesture == OverlayGesture.DOUBLE_TAP && secondTapCandidate && distance <= touchSlop) {
                            trigger()
                            lastTapTime = 0L
                        } else if (distance <= touchSlop) {
                            performClick()
                            lastTapTime = now
                            lastTapX = event.rawX
                            lastTapY = event.rawY
                            state = TouchState.WaitingSecondTap
                            handler.postDelayed({ if (state == TouchState.WaitingSecondTap) state = TouchState.Idle }, doubleTapTimeout)
                        }
                    }
                    else -> Unit
                }
                velocityTracker?.recycle()
                velocityTracker = null
                animate().alpha(config.opacity).scaleX(1f).scaleY(1f).setDuration(150).start()
                if (state != TouchState.WaitingSecondTap && state != TouchState.Cooldown) state = TouchState.Idle
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress)
                velocityTracker?.recycle()
                velocityTracker = null
                state = TouchState.Idle
                animate().alpha(config.opacity).scaleX(1f).scaleY(1f).setDuration(120).start()
            }
        }
        return true
    }

    private fun matchesSwipe(dx: Float, dy: Float, distance: Float): Boolean {
        if (config.gesture == OverlayGesture.DOUBLE_TAP || distance < width * 0.78f) return false
        val vx = velocityTracker?.xVelocity ?: 0f
        val vy = velocityTracker?.yVelocity ?: 0f
        val speedEnough = hypot(vx, vy) >= 360f || distance >= width * 1.15f
        if (!speedEnough) return false
        return when (config.gesture) {
            OverlayGesture.SWIPE_LEFT -> dx < 0 && abs(dx) > abs(dy) * 1.45f
            OverlayGesture.SWIPE_RIGHT -> dx > 0 && abs(dx) > abs(dy) * 1.45f
            OverlayGesture.SWIPE_UP -> dy < 0 && abs(dy) > abs(dx) * 1.45f
            OverlayGesture.SWIPE_DOWN -> dy > 0 && abs(dy) > abs(dx) * 1.45f
            else -> false
        }
    }

    private fun trigger() {
        val now = SystemClock.uptimeMillis()
        if (now < cooldownUntil) return
        state = TouchState.Triggered
        cooldownUntil = now + 1350L
        performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.KEYBOARD_TAP
        )
        animate().scaleX(1.10f).scaleY(1.10f).setDuration(90).withEndAction {
            animate().scaleX(1f).scaleY(1f).setDuration(130).start()
        }.start()
        callback(OverlayEvent.Trigger)
        state = TouchState.Cooldown
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        velocityTracker?.recycle()
        velocityTracker = null
        super.onDetachedFromWindow()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

private inline fun ValueAnimator.doOnEndCompat(crossinline block: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = block()
    })
}
