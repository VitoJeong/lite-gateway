package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.filter.core.GatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import reactor.core.publisher.Mono

class AddRequestFilter(
    private val map: Map<String, String>
) : GatewayFilter {

    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        val mutatedRequest = context.request.mutate().apply {
            map.forEach { (headerName, value) ->
                header(headerName, value)
            }
        }.build()

        val mutatedContext = context.mutate().request(mutatedRequest).build()

        return chain.filter(mutatedContext)
    }
}