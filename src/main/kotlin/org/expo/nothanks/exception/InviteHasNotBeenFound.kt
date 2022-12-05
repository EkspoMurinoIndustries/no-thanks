package org.expo.nothanks.exception

class InviteHasNotBeenFound(inviteCode: String): IllegalStateException("Game has not been found by invite code $inviteCode")