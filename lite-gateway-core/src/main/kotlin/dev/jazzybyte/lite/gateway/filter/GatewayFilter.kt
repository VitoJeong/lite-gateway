package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.RequestContext

interface GatewayFilter {
    fun filter(context: RequestContext)
}