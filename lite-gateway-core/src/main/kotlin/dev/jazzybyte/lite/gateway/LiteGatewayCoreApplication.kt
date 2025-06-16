package dev.jazzybyte.lite.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LiteGatewayCoreApplication

fun main(args: Array<String>) {
    runApplication<LiteGatewayCoreApplication>(*args)
}
