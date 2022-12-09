package org.expo.nothanks.exception

class InviteHasNotBeenFound(inviteCode: String) :
    NoThanksException(
        "Game has not been found by invite code: $inviteCode",
        "Invite does not exist"
    )