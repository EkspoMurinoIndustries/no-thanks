package org.expo.nothanks.model.event.output

interface OutputMessage {

    val type: String
        get() = this::class.java.simpleName

}