package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.RequestContext
import dev.jazzybyte.lite.gateway.http.GatewayHttpMethod


class MethodPredicate(
    private val text: String,
) : RoutePredicate {

    private val method: GatewayHttpMethod = GatewayHttpMethod.valueOf(text.uppercase())

    override fun matches(context: RequestContext): Boolean {
        return context.method() == method
    }

}