package org.expo.nothanks.utils

import org.expo.nothanks.exception.NoThanksException
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO
import org.expo.nothanks.model.lobby.LobbyPlayer
import java.security.MessageDigest
import java.util.UUID

fun String.avatarVersion(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

// Use the public player number, never the player's authentication token, in URLs.
fun LobbyPlayer.avatarUrl(gameId: UUID): String? = avatar?.let {
    "/avatars/$gameId/$number/${it.avatarVersion()}.jpg"
}

// Bounded thumbnails keep uploads and lobby broadcasts small on the game server.
fun String?.validatedAvatar(): String? {
    if (this == null) return null
    val prefix = "data:image/jpeg;base64,"
    try {
        require(startsWith(prefix) && length <= 8023)
        val bytes = Base64.getDecoder().decode(substring(prefix.length))
        require(bytes.size <= 6000)
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val readers = ImageIO.getImageReaders(input)
            require(readers.hasNext())
            val reader = readers.next()
            try {
                reader.input = input
                require(reader.formatName.equals("JPEG", ignoreCase = true))
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                require(width in 1..128 && height == width)
                require(reader.read(0) != null)
            } finally {
                reader.dispose()
            }
        }
        return prefix + Base64.getEncoder().encodeToString(bytes)
    } catch (_: Exception) {
        throw NoThanksException("Please choose a valid avatar image (square JPEG, up to 128 pixels and 6 KB)")
    }
}
