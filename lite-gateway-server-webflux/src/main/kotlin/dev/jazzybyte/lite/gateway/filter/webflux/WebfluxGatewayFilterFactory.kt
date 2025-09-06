package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.config.FilterRegistry
import dev.jazzybyte.lite.gateway.filter.AddRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.AddResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.GatewayFilterFactory
import dev.jazzybyte.lite.gateway.filter.RemoveRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.RemoveResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter

class WebfluxGatewayFilterFactory : GatewayFilterFactory {

    override fun create(definition: FilterDefinition): GatewayFilter {
        FilterRegistry.getFilterClass(definition.type)

        val args = definition.args
        return when (FilterRegistry.getFilterClass(definition.type)) {
            AddRequestHeaderGatewayFilter::class.java -> AddRequestHeaderGatewayFilter(
                name = args["name"] ?: throw IllegalArgumentException("name arg is missing"),
                value = args["value"] ?: throw IllegalArgumentException("value arg is missing")
            )

            RemoveRequestHeaderGatewayFilter::class.java -> RemoveRequestHeaderGatewayFilter(
                name = args["name"] ?: throw IllegalArgumentException("name arg is missing")
            )

            AddResponseHeaderGatewayFilter::class.java -> AddResponseHeaderGatewayFilter(
                name = args["name"] ?: throw IllegalArgumentException("name arg is missing"),
                value = args["value"] ?: throw IllegalArgumentException("value arg is missing")
            )

            RemoveResponseHeaderGatewayFilter::class.java -> RemoveResponseHeaderGatewayFilter(
                name = args["name"] ?: throw IllegalArgumentException("name arg is missing")
            )

            else -> throw IllegalArgumentException("Unknown filter: ${definition.type}")
        }
    }

}