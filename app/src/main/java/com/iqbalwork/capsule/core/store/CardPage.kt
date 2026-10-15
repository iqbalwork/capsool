package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.core.settings.StaticCard
import com.iqbalwork.capsule.model.CapsuleCardType

/**
 * One page in the carousel.  Dynamic pages come from live device activity; static pages
 * come from the user's configured [StaticCard] list and show when no activity is present.
 */
sealed interface CardPage {
    /** Stable identity used to track focus across recompositions. */
    val key: String

    data class Dynamic(val type: CapsuleCardType) : CardPage {
        override val key: String get() = "dyn:${type.name}"
    }

    data class Static(val card: StaticCard) : CardPage {
        override val key: String get() = "static:${card.id}"
    }
}
