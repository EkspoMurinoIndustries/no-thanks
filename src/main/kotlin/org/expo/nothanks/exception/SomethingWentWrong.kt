package org.expo.nothanks.exception

class SomethingWentWrong(msg: String = "Something went wrong") : NoThanksException("Something went wrong", msg)
