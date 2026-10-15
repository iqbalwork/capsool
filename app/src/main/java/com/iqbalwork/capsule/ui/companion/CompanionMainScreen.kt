package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.debugtools.DiagnosticsPanel
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import com.iqbalwork.capsule.ui.overlay.CapsuleRootView

@Composable
fun CompanionApp(
    state: CapsuleState,
    serviceRunning: Boolean,
    notificationAccess: Boolean,
    dispatch: (Action) -> Unit,
    onUpdateSettings: ((Settings) -> Settings) -> Unit,
    onBluetoothGranted: () -> Unit,
    onControlsPermissionsChanged: () -> Unit
) {
    if (!state.settingsLoaded) return
    var declined by rememberSaveable { mutableStateOf(false) }
    when {
        state.settings.disclosureAccepted ->
            CompanionMainScreen(
                state, serviceRunning, notificationAccess, dispatch, onUpdateSettings, onBluetoothGranted,
                onControlsPermissionsChanged
            )
        declined -> DisclosureDeclinedScreen(onReview = { declined = false })
        else -> DisclosureScreen(
            onAccept = { onUpdateSettings { it.copy(disclosureAccepted = true) } },
            onDecline = { declined = true }
        )
    }
}

@Composable
private fun CompanionMainScreen(
    state: CapsuleState,
    serviceRunning: Boolean,
    notificationAccess: Boolean,
    dispatch: (Action) -> Unit,
    onUpdateSettings: ((Settings) -> Settings) -> Unit,
    onBluetoothGranted: () -> Unit,
    onControlsPermissionsChanged: () -> Unit
) {
    val tabs = buildList {
        add("Status")
        add("Capsule")
        add("Shortcuts")
        add("Gestures")
        if (DiagnosticsPanel.isAvailable) add("Debug")
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var showNotificationApps by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (showNotificationApps) {
            NotificationAppsScreen(
                settings = state.settings,
                onUpdate = onUpdateSettings,
                onBack = { showNotificationApps = false },
                modifier = Modifier.safeDrawingPadding()
            )
            return@Surface
        }
        Column(modifier = Modifier.safeDrawingPadding()) {
            // Live preview, useful while the service is off (F-93).
            if (!serviceRunning) {
                val screenWidthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp().value }
                val previewHeight = if (state.mode == CapsuleMode.EXPANDED) OverlayPlacement.EXPANDED_HEIGHT_DP + 24 else 72
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewHeight.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CapsuleRootView(
                        state = state,
                        dispatch = dispatch,
                        expandedWidth = OverlayPlacement.expandedWidthDp(screenWidthDp).dp,
                        expandedHeight = OverlayPlacement.EXPANDED_HEIGHT_DP.dp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            PrimaryScrollableTabRow(selectedTabIndex = selected, edgePadding = 8.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selected == index, onClick = { selected = index }, text = { Text(title) })
                }
            }
            Box(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                when (tabs.getOrNull(selected)) {
                    "Status" -> StatusTab(serviceRunning = serviceRunning, notificationAccess = notificationAccess)
                    "Capsule" -> CapsuleTab(
                        settings = state.settings,
                        currentOrientation = state.visibility.orientation,
                        onUpdate = onUpdateSettings,
                        onOpenNotificationApps = { showNotificationApps = true },
                        onBluetoothGranted = onBluetoothGranted,
                        onControlsPermissionsChanged = onControlsPermissionsChanged
                    )
                    "Shortcuts" -> ShortcutsTab(settings = state.settings, onUpdate = onUpdateSettings)
                    "Gestures" -> GesturesTab(settings = state.settings, onUpdate = onUpdateSettings)
                    "Debug" -> DiagnosticsPanel.Content(dispatch = dispatch)
                }
            }
        }
    }
}
