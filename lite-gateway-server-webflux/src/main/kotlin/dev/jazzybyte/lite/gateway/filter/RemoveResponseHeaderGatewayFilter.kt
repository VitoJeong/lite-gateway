package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import dev.jazzybyte.lite.gateway.filter.core.GatewayContext
import reactor.core.publisher.Mono

class RemoveResponseHeaderGatewayFilter(private val name: String) : GatewayFilter {
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        return chain.filter(context)
            .then(Mono.fromRunnable {
                val webfluxContext = context as WebFluxGatewayContext
                val response = webfluxContext.exchange.response

                response.headers.remove(name)
            })
    }
}
