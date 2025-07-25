package dev.jazzybyte.lite.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LiteGatewayServerWebfluxApplication

fun main(args: Array<String>) {
    runApplication<LiteGatewayServerWebfluxApplication>(*args)
}
