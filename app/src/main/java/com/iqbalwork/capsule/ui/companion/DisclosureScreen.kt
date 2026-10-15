package com.iqbalwork.capsule.ui.companion

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.R

/**
 * Prominent disclosure shown before the user is sent to Accessibility settings (spec §1.6).
 * Declining keeps the app usable and never opens the settings screen.
 */
@Composable
fun DisclosureScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.disclosure_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.disclosure_intro), style = MaterialTheme.typography.bodyLarge)
            DisclosureItem(R.string.disclosure_item_overlay)
            DisclosureItem(R.string.disclosure_item_windows)
            DisclosureItem(R.string.disclosure_item_app)
            DisclosureItem(R.string.disclosure_item_actions)
            Text(
                stringResource(R.string.disclosure_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.disclosure_accept))
            }
            OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.disclosure_decline))
            }
        }
    }
}

@Composable
fun DisclosureDeclinedScreen(onReview: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.disclosure_declined_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.disclosure_declined_body), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onReview) { Text(stringResource(R.string.disclosure_review)) }
        }
    }
}

@Composable
private fun DisclosureItem(@StringRes text: Int) {
    Row {
        Text("•", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}
