package org.expo.nothanks.model.event.output

data class PlayerAvatarChangedMessage(val playerNumber: Int, val avatar: String?) : OutputMessage
