package com.iqbalwork.capsule.ui.overlay

import android.annotation.SuppressLint
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import com.iqbalwork.capsule.platform.accessibility.WindowPlacement
import com.iqbalwork.capsule.ui.theme.OverlayTheme

/** Content of the overlay window; the window itself is sized by OverlayWindowController. */
@Composable
fun OverlayRoot(
    state: CapsuleState,
    placement: WindowPlacement?,
    dispatch: (Action) -> Unit
) {
    if (placement == null || state.mode == CapsuleMode.HIDDEN) return
    // The overlay window is only as big as the capsule, so window-size APIs would be wrong here.
    @SuppressLint("ConfigurationScreenWidthHeight")
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()

    // The capsule and its cards have fixed sizes (they must line up with the camera cutout), so
    // text is allowed to grow with the system font size only until it would start to clip.
    val density = LocalDensity.current
    val cappedDensity = remember(density) {
        Density(density.density, fontScale = density.fontScale.coerceAtMost(MAX_OVERLAY_FONT_SCALE))
    }
    CompositionLocalProvider(LocalDensity provides cappedDensity) {
        OverlayTheme(colorSource = state.settings.colorSource) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                CapsuleRootView(
                    state = state,
                    dispatch = dispatch,
                    expandedWidth = OverlayPlacement.expandedWidthDp(screenWidthDp).dp,
                    expandedHeight = OverlayPlacement.expandedContentHeightDp(state.listRows).dp,
                    modifier = Modifier
                        .offset { IntOffset(placement.contentOffsetXPx, placement.contentOffsetYPx) }
                        .padding(top = OverlayPlacement.MARGIN_DP.dp)
                )
            }
        }
    }
}

/** Text in the overlay stops growing here; the companion app has no such limit. */
private const val MAX_OVERLAY_FONT_SCALE = 1.3f
