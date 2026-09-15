package org.expo.nothanks.utils

import org.expo.nothanks.exception.SomethingWentWrong
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class PlayerNameUtilsTest {

    @Test
    fun `preserves letter case`() {
        assertEquals("McCloud", "McCloud".validatedPlayerName())
    }

    @Test
    fun `limits name length`() {
        assertEquals("123456789012345", "1234567890123456".validatedPlayerName())
    }

    @Test
    fun `rejects blank name`() {
        assertThrows(SomethingWentWrong::class.java) {
            "   ".validatedPlayerName()
        }
    }
}
