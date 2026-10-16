package com.iqbalwork.capsule.core

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Text produced by code that has no Context (validators, reducers) and is shown later, so it is
 * translated when it is displayed rather than when it is created. Keeps that code pure and lets
 * tests assert on the resource id and arguments instead of on English or Indonesian wording.
 */
sealed interface UiText {
    /** A string resource with optional format arguments. */
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Text that is not ours to translate, such as a system error message. */
    data class Raw(val value: String) : UiText

    fun resolve(context: Context): String = when (this) {
        is Res -> context.getString(id, *args.toTypedArray())
        is Raw -> value
    }

    @Composable
    fun asString(): String = when (this) {
        is Res -> stringResource(id, *args.toTypedArray())
        is Raw -> value
    }

    companion object {
        fun res(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}
