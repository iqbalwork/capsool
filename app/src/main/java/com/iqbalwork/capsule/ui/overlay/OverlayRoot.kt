package com.iqbalwork.capsule.ui.overlay

import android.annotation.SuppressLint
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
