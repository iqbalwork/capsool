package com.iqbalwork.capsule.platform.accessibility

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Region
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.StaticCardType
import com.iqbalwork.capsule.core.store.CardPage
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleStore
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.platform.barcode.BarcodeValidator
import com.iqbalwork.capsule.ui.overlay.OverlayRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "CapsoolOverlay"

/**
 * Keep the window at card size until the collapse spring has visibly settled; shrinking
 * earlier clips the still-moving edge (CollapseSpring: damping 0.8, stiffness 260).
 */
private const val SHRINK_DELAY_MS = 750L

/**
 * Owns the single TYPE_ACCESSIBILITY_OVERLAY window (spec §2.4.2). The window hugs the
 * capsule while collapsed so touches elsewhere reach the app below; it is resized once
 * before expanding and once after collapsing, never per animation frame.
 */
class OverlayWindowController(
    private val context: Context,
    private val store: CapsuleStore,
    private val scope: CoroutineScope,
    private val onStatusBarVisibilityChanged: (visible: Boolean) -> Unit
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val lifecycleOwner = OverlayLifecycleOwner()
    private var view: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var layoutJob: Job? = null
    private var shrinkJob: Job? = null
    private var windowExpanded = false
    private var lastLayout: LayoutKey? = null
    private val placement = mutableStateOf<WindowPlacement?>(null)

    private data class LayoutKey(
        val mode: CapsuleMode,
        val geometry: Geometry,
        val focusable: Boolean,
        val barcodeShowing: Boolean = false
    )

    fun attach() {
        if (view != null) return
        lifecycleOwner.start()
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                val state by store.state.collectAsStateWithLifecycle()
                val currentPlacement by placement
                OverlayRoot(
                    state = state,
                    placement = currentPlacement,
                    dispatch = store::dispatch
                )
            }
        }
        installTouchAndInsetListeners(composeView)

        val layoutParams = WindowManager.LayoutParams(
            0,
            0,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Anchored on the horizontal center: resizing between pill and card then never
            // moves the content sideways, even for the frame before it is laid out again.
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = "Capsool"
        }

        try {
            windowManager.addView(composeView, layoutParams)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not add overlay window", e)
            lifecycleOwner.destroy()
            return
        }
        view = composeView
        params = layoutParams

        layoutJob = scope.launch {
            store.state
                .map {
                    val barcodeShowing = it.mode == CapsuleMode.EXPANDED &&
                        it.settings.barcode.enabled &&
                        BarcodeValidator.validate(it.settings.barcode.content, it.settings.barcode.format).isValid &&
                        // Keyed on the page, not on activeCard: a static Barcode card makes
                        // activeCard null, and without the boost the code is hard to scan.
                        when (val page = it.activePage) {
                            is CardPage.Static -> page.card.type == StaticCardType.BARCODE ||
                                (page.card.type == StaticCardType.SHORTCUTS && it.settings.barcodeCardEnabled)
                            is CardPage.Dynamic -> page.type == CapsuleCardType.SHORTCUTS && it.settings.barcodeCardEnabled
                            null -> false
                        }
                    LayoutKey(
                        mode = it.mode,
                        geometry = it.settings.geometry(it.visibility.orientation),
                        focusable = it.replyingTo != null,
                        barcodeShowing = barcodeShowing
                    )
                }
                .distinctUntilChanged()
                .collect { applyLayout(it) }
        }
    }

    /** Call after rotation or display changes so the window follows the cutout. */
    fun refresh() {
        lastLayout?.let { place(it, expanded = windowExpanded) }
    }

    fun detach() {
        layoutJob?.cancel()
        shrinkJob?.cancel()
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        params = null
        lifecycleOwner.destroy()
    }

    private fun applyLayout(key: LayoutKey) {
        lastLayout = key
        shrinkJob?.cancel()
        if (FIXED_SIZE_WINDOW) {
            place(key, expanded = key.mode == CapsuleMode.EXPANDED)
            return
        }
        when (key.mode) {
            CapsuleMode.EXPANDED -> place(key, expanded = true)
            else -> if (windowExpanded) {
                shrinkJob = scope.launch {
                    delay(scaledShrinkDelay())
                    place(key, expanded = false)
                }
            } else {
                place(key, expanded = false)
            }
        }
    }

    private fun place(key: LayoutKey, expanded: Boolean) {
        val composeView = view ?: return
        val layoutParams = params ?: return
        val metrics = windowManager.currentWindowMetrics
        val density = context.resources.displayMetrics.density
        val newPlacement = OverlayPlacement.compute(
            screenWidthPx = metrics.bounds.width(),
            density = density,
            cutout = readCutout(),
            geometry = key.geometry,
            // API 34+: the window keeps card size and only its touchable region changes.
            expanded = expanded || FIXED_SIZE_WINDOW,
            miniEvent = key.mode == CapsuleMode.MINI_EVENT
        )
        windowExpanded = expanded
        placement.value = newPlacement

        val visible = key.mode != CapsuleMode.HIDDEN
        // An empty composition does not clear the window's last frame; hiding the root view
        // makes WindowManager hide the surface itself.
        composeView.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        layoutParams.x = newPlacement.x + newPlacement.width / 2 - metrics.bounds.width() / 2
        layoutParams.y = newPlacement.y
        layoutParams.width = newPlacement.width
        layoutParams.height = newPlacement.height
        // The reply field (spec §2.4.2) needs keyboard focus; otherwise the overlay never takes it.
        var flags = if (visible) BASE_FLAGS else BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        if (key.focusable) flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        layoutParams.flags = flags
        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        layoutParams.screenBrightness = if (key.barcodeShowing) 1f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        runCatching { windowManager.updateViewLayout(composeView, layoutParams) }
            .onFailure { Log.w(TAG, "Overlay relayout failed", it) }
        if (FIXED_SIZE_WINDOW) {
            val rect = OverlayPlacement.capsuleRect(
                newPlacement, density, key.geometry, expanded, miniEvent = key.mode == CapsuleMode.MINI_EVENT
            )
            val region = if (visible) Region(rect.left, rect.top, rect.right, rect.bottom) else Region()
            setTouchableRegion(composeView, region)
        }
    }

    /**
     * Touches outside [region] fall through to the app below even though the window is card
     * sized, so the window never has to be resized (a resize shows one frame of the old content
     * in the new frame, which made the collapsing pill jump sideways).
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun setTouchableRegion(composeView: ComposeView, region: Region) {
        val surface = composeView.rootSurfaceControl
        if (surface != null) {
            surface.setTouchableRegion(region)
        } else {
            composeView.post { composeView.rootSurfaceControl?.setTouchableRegion(region) }
        }
    }

    /** Compose animations follow the system animator scale, so the shrink delay must too. */
    private fun scaledShrinkDelay(): Long {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return (SHRINK_DELAY_MS * scale).toLong()
    }

    /**
     * Bounding rects are anchored to the screen edge (on a Pixel the top rect starts at y=0 even
     * though the hole sits lower), so prefer the tight bounds of the cutout path when it exists.
     */
    private fun readCutout(): CutoutBounds? {
        val cutout = windowManager.currentWindowMetrics.windowInsets.displayCutout ?: return null
        val top = cutout.boundingRectTop.takeUnless { it.isEmpty } ?: return null
        val pathBounds = cutout.cutoutPath?.let { path -> RectF().also { path.computeBounds(it, true) } }
            ?.takeIf { !it.isEmpty && RectF(top).contains(it.centerX(), it.centerY()) }
        return if (pathBounds != null) {
            CutoutBounds(pathBounds.left.toInt(), pathBounds.top.toInt(), pathBounds.right.toInt(), pathBounds.bottom.toInt())
        } else {
            CutoutBounds(top.left, top.top, top.right, top.bottom)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun installTouchAndInsetListeners(composeView: ComposeView) {
        composeView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) store.dispatch(UserIntent.Collapse)
            false
        }
        // Fullscreen detection (spec §2.4.4): needs validation per OEM, see risk register.
        ViewCompat.setOnApplyWindowInsetsListener(composeView) { _, insets ->
            onStatusBarVisibilityChanged(insets.isVisible(WindowInsetsCompat.Type.statusBars()))
            insets
        }
    }

    private companion object {
        /** Android 14+ can limit touches to a region, so the window keeps one size. */
        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        val FIXED_SIZE_WINDOW = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        const val BASE_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }
}
