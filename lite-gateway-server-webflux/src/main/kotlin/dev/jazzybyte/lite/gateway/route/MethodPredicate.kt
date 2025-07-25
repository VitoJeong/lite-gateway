package dev.jazzybyte.lite.gateway.route

import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange


class MethodPredicate(
    private val text: String,
) : RoutePredicate {

    private val method: HttpMethod = HttpMethod.valueOf(text.uppercase())

    override fun matches(exchange: ServerWebExchange): Boolean {
        return exchange.request.method == method
    }

}