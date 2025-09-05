package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.config.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.GatewayFilterFactory
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.global.AddRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.global.AddResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.global.RemoveRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.global.RemoveResponseHeaderGatewayFilter

class WebfluxGatewayFilterFactory : GatewayFilterFactory {
    override fun create(filterDefinition: FilterDefinition): GatewayFilter {
        val args = filterDefinition.args
        return when (filterDefinition.name) {
            "AddRequestHeader" -> AddRequestHeaderGatewayFilter(
                name = args["name"] ?: throw IllegalArgumentException("name arg is missing"),
                value = args["value"] ?: throw IllegalArgumentException("value arg is missing")
            )
            "RemoveRequestHeader" -> RemoveRequestHeaderGatewayFilter(
                name = args["name"] ?: throw IllegalArgumentException("name arg is missing")
            )
            "AddResponseHeader" -> AddResponseHeaderGatewayFilter(
                name = args["name"] ?: throw IllegalArgumentException("name arg is missing"),
                value = args["value"] ?: throw IllegalArgumentException("value arg is missing")
            )
            "RemoveResponseHeader" -> RemoveResponseHeaderGatewayFilter(
                name = args["name"] ?: throw IllegalArgumentException("name arg is missing")
            )
            else -> throw IllegalArgumentException("Unknown filter: ${filterDefinition.name}")
        }
    }
}