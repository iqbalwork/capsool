package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.iqbalwork.capsule.core.settings.CustomIntent
import com.iqbalwork.capsule.core.settings.CustomIntentType
import com.iqbalwork.capsule.core.settings.CustomIntentValidator
import com.iqbalwork.capsule.core.settings.CustomIntentValidator.isValid
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureBinding
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.UserIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomIntentTest {

    // --- Encode / Decode Round-Trip ---

    @Test
    fun roundTripEncodingWithMultiLineAndSpecialCharacters() {
        val multilineExtras = mapOf(
            "message" to "Hello world!\nSecond line with indentation\n\tTabbed line\r\nCarriage return",
            "json" to """{"user": "alice", "active": true, "tags": ["a", "b"]}""",
            "special_symbols" to "!@#$%^&*()_+-=[]{}|;':\",.<>?/~` 🚀⚡",
            "empty_val" to "",
            "equals_in_val" to "key=value&other=123"
        )

        val intents = listOf(
            CustomIntent(
                id = "intent-1",
                label = "Trigger MacroDroid",
                type = CustomIntentType.BROADCAST,
                action = "com.macrodroid.ACTION_EVENT",
                component = "com.arlosoft.macrodroid/.MacroDroidReceiver",
                extras = multilineExtras,
                icon = "bolt"
            ),
            CustomIntent(
                id = "intent-2",
                label = "Open Browser",
                type = CustomIntentType.ACTIVITY,
                action = "android.intent.action.VIEW",
                component = null,
                extras = emptyMap(),
                icon = "star"
            ),
            CustomIntent(
                id = "intent-3",
                label = "Start Sync Service",
                type = CustomIntentType.SERVICE,
                action = "com.example.SYNC",
                component = "com.example/com.example.sync.SyncService",
                extras = mapOf("force" to "true"),
                icon = "refresh"
            )
        )

        val encoded = CustomIntent.encodeAll(intents)
        val decoded = CustomIntent.decodeAll(encoded)

        assertEquals(intents, decoded)
    }

    @Test
    fun deterministicExtrasEncoding() {
        val extras1 = sortedMapOf("z" to "last", "a" to "first", "m" to "middle")
        val extras2 = linkedMapOf("a" to "first", "m" to "middle", "z" to "last")

        val intent1 = CustomIntent("id", "label", CustomIntentType.BROADCAST, "act", extras = extras1)
        val intent2 = CustomIntent("id", "label", CustomIntentType.BROADCAST, "act", extras = extras2)

        assertEquals(CustomIntent.encodeAll(listOf(intent1)), CustomIntent.encodeAll(listOf(intent2)))
    }

    @Test
    fun skipCorruptedRecordsWithoutCrashing() {
        assertTrue(CustomIntent.decodeAll(null).isEmpty())
        assertTrue(CustomIntent.decodeAll("").isEmpty())
        assertTrue(CustomIntent.decodeAll("   ").isEmpty())
        assertTrue(CustomIntent.decodeAll("completely_invalid_record").isEmpty())
        // Invalid type enum
        assertTrue(CustomIntent.decodeAll("id\u001Flabel\u001FINVALID_TYPE\u001Faction\u001Fcomp\u001Fextras").isEmpty())
        // Empty action
        assertTrue(CustomIntent.decodeAll("id\u001Flabel\u001FACTIVITY\u001F   \u001Fcomp\u001Fextras").isEmpty())
        // Empty ID
        assertTrue(CustomIntent.decodeAll("\u001Flabel\u001FACTIVITY\u001Faction\u001Fcomp\u001Fextras").isEmpty())

        // Mixed valid and corrupted: returns the valid one
        val valid = CustomIntent("ok-1", "Valid", CustomIntentType.BROADCAST, "com.ok")
        val mixedEncoded = "garbage\u001E" + CustomIntent.encodeAll(listOf(valid)) + "\u001Ebad\u001Ftoo_few_fields"
        val decoded = CustomIntent.decodeAll(mixedEncoded)
        assertEquals(listOf(valid), decoded)
    }

    // --- Gesture Binding Encoding ---

    @Test
    fun gestureBindingWithCustomIntentRoundTrip() {
        val binding = GestureBinding(GestureAction.CUSTOM_INTENT, "custom-id-42")
        val encoded = binding.encode()
        assertEquals("CUSTOM_INTENT:custom-id-42", encoded)

        val decoded = GestureBinding.decode(encoded)
        assertEquals(binding, decoded)

        // Null target
        val nullTargetBinding = GestureBinding(GestureAction.CUSTOM_INTENT, null)
        assertEquals("CUSTOM_INTENT", nullTargetBinding.encode())
        assertEquals(nullTargetBinding, GestureBinding.decode("CUSTOM_INTENT"))
    }

    // --- Reducer Behavior ---

    @Test
    fun reducerEmitsSendCustomIntentWhenIntentExists() {
        val intent = CustomIntent("action-99", "Run Task", CustomIntentType.BROADCAST, "com.action.RUN")
        val binding = GestureBinding(GestureAction.CUSTOM_INTENT, "action-99")
        val settings = Settings(
            customIntents = listOf(intent),
            gesturesActive = mapOf(GestureType.TAP to binding),
            gesturesIdle = mapOf(GestureType.TAP to binding)
        )
        val state = CapsuleState(settingsLoaded = true, settings = settings, isExpanded = true)

        val reduction = CapsuleReducer.reduce(state, UserIntent.Gesture(GestureType.TAP))

        assertEquals(listOf(Effect.SendCustomIntent("action-99")), reduction.effects)
        assertFalse("Capsule should collapse when custom intent is triggered", reduction.state.isExpanded)
    }

    @Test
    fun reducerEmitsNoEffectWhenIntentDoesNotExist() {
        val binding = GestureBinding(GestureAction.CUSTOM_INTENT, "missing-id")
        val settings = Settings(
            customIntents = listOf(CustomIntent("other-id", "Other", CustomIntentType.BROADCAST, "act")),
            gesturesActive = mapOf(GestureType.TAP to binding),
            gesturesIdle = mapOf(GestureType.TAP to binding)
        )
        val state = CapsuleState(settingsLoaded = true, settings = settings)

        val reduction = CapsuleReducer.reduce(state, UserIntent.Gesture(GestureType.TAP))

        assertTrue(reduction.effects.isEmpty())
        assertEquals(state, reduction.state)
    }

    @Test
    fun reducerEmitsNoEffectWhenTargetIsNull() {
        val binding = GestureBinding(GestureAction.CUSTOM_INTENT, null)
        val settings = Settings(
            customIntents = listOf(CustomIntent("some-id", "Some", CustomIntentType.BROADCAST, "act")),
            gesturesActive = mapOf(GestureType.TAP to binding),
            gesturesIdle = mapOf(GestureType.TAP to binding)
        )
        val state = CapsuleState(settingsLoaded = true, settings = settings)

        val reduction = CapsuleReducer.reduce(state, UserIntent.Gesture(GestureType.TAP))

        assertTrue(reduction.effects.isEmpty())
        assertEquals(state, reduction.state)
    }

    // --- Validator Pure Tests ---

    @Test
    fun validatorRejectsEmptyAction() {
        assertFalse(CustomIntentValidator.validate("", null).isValid)
        assertFalse(CustomIntentValidator.validate("   ", null).isValid)
        assertTrue(CustomIntentValidator.validate("android.intent.action.VIEW", null).isValid)
    }

    @Test
    fun validatorComponentFormat() {
        // Valid component formats
        assertTrue(CustomIntentValidator.validate("com.act", null).isValid)
        assertTrue(CustomIntentValidator.validate("com.act", "").isValid)
        assertTrue(CustomIntentValidator.validate("com.act", "   ").isValid)
        assertTrue(CustomIntentValidator.validate("com.act", "com.example/.MyReceiver").isValid)
        assertTrue(CustomIntentValidator.validate("com.act", "com.example/com.example.MyReceiver").isValid)

        // Invalid component formats
        assertFalse(CustomIntentValidator.validate("com.act", "com.example").isValid)
        assertFalse(CustomIntentValidator.validate("com.act", "/.MyReceiver").isValid)
        assertFalse(CustomIntentValidator.validate("com.act", "com.example/").isValid)
        assertFalse(CustomIntentValidator.validate("com.act", "com.example/pkg/cls").isValid)
    }

    // --- SettingsMapper Round-Trip ---

    @Test
    fun settingsMapperPersistsCustomIntents() {
        val intent = CustomIntent(
            id = "test-intent-id",
            label = "Test Action",
            type = CustomIntentType.SERVICE,
            action = "com.test.START_SERVICE",
            component = "com.test/.MyService",
            extras = mapOf("mode" to "fast", "multiline" to "a\nb"),
            icon = "flash"
        )
        val original = Settings(customIntents = listOf(intent))

        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, original)
        val restored = SettingsMapper.read(prefs)

        assertEquals(listOf(intent), restored.customIntents)
    }
}
