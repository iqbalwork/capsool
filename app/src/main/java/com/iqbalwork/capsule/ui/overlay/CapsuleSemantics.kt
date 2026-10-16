package com.iqbalwork.capsule.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.iqbalwork.capsule.R
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.UserIntent

/**
 * Screen-reader access to the capsule. Its gestures (swipe, double tap, long press) cannot be made
 * with TalkBack, so everything they do is offered as accessibility actions that run the same
 * reducer paths with fixed meanings instead of the user's gesture bindings.
 *
 * Collapsed, the capsule is one element that says what it shows and announces changes (a new
 * notification) politely. Expanded, the card's own content stays individually focusable and the
 * capsule only adds its actions.
 */
@Composable
fun Modifier.capsuleSemantics(state: CapsuleState, expanded: Boolean, dispatch: (Action) -> Unit): Modifier {
    val summary = capsuleSummary(state).asString()
    val toggleLabel = stringResource(if (expanded) R.string.a11y_action_collapse else R.string.a11y_action_expand)
    val nextLabel = stringResource(R.string.a11y_action_next_card)
    val previousLabel = stringResource(R.string.a11y_action_previous_card)
    val playPauseLabel = stringResource(R.string.a11y_action_play_pause)
    val hasMedia = state.media != null
    val severalPages = state.pages.size > 1

    fun perform(action: GestureAction): Boolean {
        dispatch(UserIntent.Perform(action))
        return true
    }

    val actions = buildList {
        if (severalPages) {
            add(CustomAccessibilityAction(nextLabel) { perform(GestureAction.NEXT_CARD) })
            add(CustomAccessibilityAction(previousLabel) { perform(GestureAction.PREVIOUS_CARD) })
        }
        if (hasMedia) add(CustomAccessibilityAction(playPauseLabel) { perform(GestureAction.MEDIA_PLAY_PAUSE) })
    }

    val common: androidx.compose.ui.semantics.SemanticsPropertyReceiver.() -> Unit = {
        contentDescription = summary
        role = Role.Button
        onClick(label = toggleLabel) { perform(GestureAction.TOGGLE_EXPAND) }
        customActions = actions
    }
    return if (expanded) {
        semantics(properties = common)
    } else {
        clearAndSetSemantics {
            common()
            liveRegion = LiveRegionMode.Polite
        }
    }
}
