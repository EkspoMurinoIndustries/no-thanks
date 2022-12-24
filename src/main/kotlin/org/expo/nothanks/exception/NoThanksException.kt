package org.expo.nothanks.exception

open class NoThanksException(message: String, val publicMessage: String = message): IllegalStateException(message) {

    val type: String
        get() = this::class.java.simpleName

}