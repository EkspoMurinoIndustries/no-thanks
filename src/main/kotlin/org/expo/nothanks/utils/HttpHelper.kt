package org.expo.nothanks.utils

import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import java.security.Principal
import java.util.*

fun ServerHttpRequest.getCookies(): Map<String, String> {
    val cookies = this.headers[HttpHeaders.COOKIE]?.get(0) ?: return emptyMap()
    return cookies.split(";")
        .map { it.trim() }
        .map { it.split("=") }
        .filter { it.size == 2 }
        .associate { it[0] to it[1] }
}

fun Principal.getPlayerId(): UUID {
    return UUID.fromString(this.name)
}