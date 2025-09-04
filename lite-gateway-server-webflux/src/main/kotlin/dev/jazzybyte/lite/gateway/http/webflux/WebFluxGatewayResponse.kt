package dev.jazzybyte.lite.gateway.http.webflux

import dev.jazzybyte.lite.gateway.filter.core.GatewayResponse
import dev.jazzybyte.lite.gateway.filter.core.GatewayResponseBuilder
import org.springframework.http.HttpStatusCode
import org.springframework.http.server.reactive.ServerHttpResponse

/**
 * Spring WebFlux의 ServerHttpResponse를 GatewayResponse 인터페이스에 맞게 래핑하는 어댑터 클래스.
 */
class WebFluxGatewayResponse(private val response: ServerHttpResponse) : GatewayResponse {
    override var statusCode: Int
        get() = response.statusCode?.value() ?: 0
        set(value) {
            response.statusCode = HttpStatusCode.valueOf(value)
        }

    override fun setHeader(name: String, value: String) {
        response.headers.set(name, value)
    }

    override fun mutate(): GatewayResponseBuilder {
        return WebFluxGatewayResponseBuilder(this.response)
    }
}

/**
 * Spring WebFlux의 ServerHttpResponse를 GatewayResponseBuilder 인터페이스에 맞게 래핑하는 어댑터 클래스.
 */
class WebFluxGatewayResponseBuilder(private val response: ServerHttpResponse) : GatewayResponseBuilder {
    override fun statusCode(statusCode: Int): GatewayResponseBuilder {
        response.statusCode = HttpStatusCode.valueOf(statusCode)
        return this
    }

    override fun header(name: String, vararg values: String): GatewayResponseBuilder {
        response.headers.set(name, values.toList())
        return this
    }

    override fun build(): GatewayResponse {
        return WebFluxGatewayResponse(response)
    }
}
