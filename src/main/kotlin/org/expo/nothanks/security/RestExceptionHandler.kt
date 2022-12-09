package org.expo.nothanks.security

import mu.KLogging
import org.expo.nothanks.exception.NoThanksException
import org.expo.nothanks.model.RestErrorMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.util.*

@RestControllerAdvice
class RestExceptionHandler {


    @ExceptionHandler(NoThanksException::class)
    fun handleException(
        request: WebRequest,
        ex: NoThanksException
    ): ResponseEntity<RestErrorMessage> {
        logger.warn("Http exception", ex)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                RestErrorMessage(
                    message = ex.publicMessage
                )
            )
    }

    private companion object : KLogging()

}