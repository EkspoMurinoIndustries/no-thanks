package org.expo.nothanks

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan("org.expo.nothanks.config.properties")
class NoThanksApplication

fun main(args: Array<String>) {
    runApplication<NoThanksApplication>(*args)
}
