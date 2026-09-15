package org.expo.nothanks.utils

import org.expo.nothanks.exception.SomethingWentWrong

const val MAX_PLAYER_NAME_LENGTH = 15

fun String.validatedPlayerName(): String {
    if (isBlank()) {
        throw SomethingWentWrong("Name cannot be blank")
    }
    return take(MAX_PLAYER_NAME_LENGTH)
}
