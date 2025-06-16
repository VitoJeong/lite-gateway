package dev.jazzybyte.lite.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LiteGatewaySpringBootStarterApplication

fun main(args: Array<String>) {
    runApplication<LiteGatewaySpringBootStarterApplication>(*args)
}
