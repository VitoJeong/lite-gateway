package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import dev.jazzybyte.lite.gateway.filter.core.GatewayContext
import reactor.core.publisher.Mono

class RemoveRequestHeaderGatewayFilter(private val name: String) : GatewayFilter {
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        val webfluxContext = context as WebFluxGatewayContext
        val request = webfluxContext.exchange.request

        val newRequest = request.mutate()
            .headers { it.remove(name) }
            .build()

        webfluxContext.exchange = webfluxContext.exchange.mutate().request(newRequest).build()

        return chain.filter(context)
    }
}
