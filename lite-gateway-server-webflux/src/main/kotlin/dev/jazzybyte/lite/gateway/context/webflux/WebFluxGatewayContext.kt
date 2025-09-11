package dev.jazzybyte.lite.gateway.context.webflux

import dev.jazzybyte.lite.gateway.filter.GatewayContext
import dev.jazzybyte.lite.gateway.filter.GatewayContextBuilder
import dev.jazzybyte.lite.gateway.filter.GatewayRequest
import dev.jazzybyte.lite.gateway.filter.GatewayResponse
import dev.jazzybyte.lite.gateway.http.webflux.WebFluxGatewayRequest
import dev.jazzybyte.lite.gateway.http.webflux.WebFluxGatewayResponse
import dev.jazzybyte.lite.gateway.route.Route
import org.springframework.web.server.ServerWebExchange

/**
 * Spring WebFlux의 ServerWebExchange를 GatewayContext 인터페이스에 맞게 래핑하는 어댑터 클래스.
 * core 모듈의 필터들이 Spring 객체에 직접 의존하지 않고 작업할 수 있도록 한다.
 */
class WebFluxGatewayContext(
    var exchange: ServerWebExchange,
    val matchedRoute: Route
) : GatewayContext {

    override val request: GatewayRequest by lazy { WebFluxGatewayRequest(exchange.request) }
    override val response: GatewayResponse by lazy { WebFluxGatewayResponse(exchange.response) }

    override fun getAttribute(name: String): Any? {
        // 매칭된 라우트와 같은 핵심 속성은 직접 제공하거나 Spring의 attributes에서 가져온다.
        // 여기서는 Route 객체를 직접 전달받아 사용한다.
        if (name == "matchedRoute") { // GatewayHandlerMapping.MATCHED_ROUTE_ATTRIBUTE 에 해당하는 문자열
            return matchedRoute
        }
        return exchange.attributes[name]
    }

    override fun putAttribute(name: String, value: Any) {
        exchange.attributes[name] = value
    }

    override fun mutate(): GatewayContextBuilder {
        return WebFluxGatewayContextBuilder(this.exchange.mutate(), this.matchedRoute)
    }
}

/**
 * Spring WebFlux의 ServerWebExchange.Builder를 GatewayContextBuilder 인터페이스에 맞게 래핑하는 어댑터 클래스.
 */
class WebFluxGatewayContextBuilder(
    private val builder: ServerWebExchange.Builder,
    private val matchedRoute: Route
) : GatewayContextBuilder {

    private var currentRequest: GatewayRequest? = null
    private var currentResponse: GatewayResponse? = null
    private val attributes: MutableMap<String, Any> = mutableMapOf()

    override fun request(request: GatewayRequest): GatewayContextBuilder {
        if (request is WebFluxGatewayRequest) {
            builder.request(request.request) // Set the underlying ServerHttpRequest
        }
        currentRequest = request
        return this
    }

    override fun response(response: GatewayResponse): GatewayContextBuilder {
        if (response is WebFluxGatewayResponse) {
            // ServerHttpResponse cannot be directly set on ServerWebExchange.Builder
            // This might require a different approach or be handled implicitly by Spring.
            // For now, we'll just store it.
        }
        currentResponse = response
        return this
    }

    override fun attribute(name: String, value: Any): GatewayContextBuilder {
        attributes[name] = value
        return this
    }

    override fun build(): GatewayContext {
        val newExchange = builder.build()
        // Apply attributes to the new exchange
        attributes.forEach { (name, value) ->
            newExchange.attributes[name] = value
        }
        return WebFluxGatewayContext(newExchange, matchedRoute)
    }
}
