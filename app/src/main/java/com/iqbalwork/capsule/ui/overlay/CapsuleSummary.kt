package com.iqbalwork.capsule.ui.overlay

import com.iqbalwork.capsule.R
import com.iqbalwork.capsule.core.UiText
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.CardPage
import com.iqbalwork.capsule.model.CapsuleCardType

/**
 * What a screen reader says for the capsule as a whole. The capsule's gestures cannot be used with
 * TalkBack, so this is the one description of whatever it is showing; its content respects the
 * same lock-screen privacy rule as the card ([CapsuleState.showsText]).
 */
fun capsuleSummary(state: CapsuleState): UiText {
    state.miniEvent?.takeIf { !state.isExpanded }?.let { event ->
        return if (event.subtitle.isBlank()) UiText.res(R.string.a11y_summary_event, event.title)
        else UiText.res(R.string.a11y_summary_event_detail, event.title, event.subtitle)
    }
    val page = state.activePage ?: return UiText.res(R.string.a11y_summary_idle)
    val detail = when (page) {
        is CardPage.Static -> UiText.res(R.string.a11y_card_static, page.card.label)
        is CardPage.Dynamic -> dynamicDetail(state, page.type)
    }
    return if (state.isExpanded) UiText.res(R.string.a11y_summary_expanded, detail)
    else UiText.res(R.string.a11y_summary_collapsed, detail)
}

private fun dynamicDetail(state: CapsuleState, type: CapsuleCardType): UiText = when (type) {
    CapsuleCardType.NOTIFICATION -> state.topNotification?.let { n ->
        if (state.showsText(n)) UiText.res(R.string.a11y_card_notification_text, n.appName, n.sender, n.text)
        else UiText.res(R.string.a11y_card_notification, n.appName)
    } ?: generic(type)
    CapsuleCardType.MEDIA -> state.media?.let { m ->
        UiText.res(if (m.isPlaying) R.string.a11y_card_media_playing else R.string.a11y_card_media_paused, m.title, m.artist)
    } ?: generic(type)
    CapsuleCardType.CHARGING -> UiText.res(R.string.a11y_card_charging, state.charging.chargePercentage)
    CapsuleCardType.TIMER -> state.timer?.let { UiText.res(R.string.a11y_card_timer, it.label) }
        ?: generic(type)
    else -> generic(type)
}

private fun generic(type: CapsuleCardType): UiText =
    UiText.res(R.string.a11y_card_generic, UiText.res(type.labelRes))
