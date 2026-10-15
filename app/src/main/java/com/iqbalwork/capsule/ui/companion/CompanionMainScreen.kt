package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.engine.preferences.CutoutPreferencesRepository
import com.iqbalwork.capsule.engine.state.CapsuleStateEngine
import com.iqbalwork.capsule.ui.CapsuleRootView
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun CompanionMainScreen(
    engine: CapsuleStateEngine = CapsuleStateEngine.instance,
    prefsRepo: CutoutPreferencesRepository? = null,
    modifier: Modifier = Modifier
) {
    val state by engine.state.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cards Stack", "Calibration", "Diagnostics")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CapsuleTokens.SurfaceContainerLowest,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CapsuleTokens.Surface)
                    .padding(top = 16.dp)
            ) {
                // Interactive In-App Capsule Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (state.isExpanded) 200.dp else 60.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CapsuleRootView(engine = engine)
                }

                // Tab Navigation
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = CapsuleTokens.Surface,
                    contentColor = CapsuleTokens.Primary,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = CapsuleTokens.Primary
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) CapsuleTokens.Primary else CapsuleTokens.OnSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> FeaturesTab(
                    state = state,
                    onAction = { engine.dispatch(it) }
                )
                1 -> CapsuleSettingsTab(
                    state = state,
                    onAction = { engine.dispatch(it) },
                    prefsRepo = prefsRepo
                )
                2 -> AppSettingsTab(
                    state = state,
                    onAction = { engine.dispatch(it) }
                )
            }
        }
    }
}
