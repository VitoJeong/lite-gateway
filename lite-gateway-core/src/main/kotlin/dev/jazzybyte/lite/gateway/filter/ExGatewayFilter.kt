package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.RequestContext

interface ExGatewayFilter {
    fun filter(context: RequestContext)
}