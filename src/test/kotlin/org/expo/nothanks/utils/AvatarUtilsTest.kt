package org.expo.nothanks.utils

import org.expo.nothanks.exception.NoThanksException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

internal class AvatarUtilsTest {
    private fun image(width: Int, height: Int, format: String = "jpeg"): String {
        val output = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, output)
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(output.toByteArray())
    }

    @Test
    fun `accepts small square JPEG thumbnails and no avatar`() {
        val avatar = image(96, 96)
        assertEquals(avatar, avatar.validatedAvatar())
        assertNull(null.validatedAvatar())
    }

    @Test
    fun `rejects malformed oversized and non JPEG avatars`() {
        for (invalid in listOf(
            "https://example.com/avatar.jpg", "data:image/svg+xml;base64,PHN2Zz4=",
            "data:image/jpeg;base64,broken", "data:image/jpeg;base64," + "A".repeat(8100),
            image(129, 129), image(96, 80), image(96, 96, "png")
        )) {
            assertThrows(NoThanksException::class.java) { invalid.validatedAvatar() }
        }
    }
}
