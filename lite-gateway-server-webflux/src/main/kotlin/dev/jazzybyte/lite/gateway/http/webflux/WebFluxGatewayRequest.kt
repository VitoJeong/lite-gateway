package dev.jazzybyte.lite.gateway.http.webflux

import dev.jazzybyte.lite.gateway.filter.core.GatewayRequest
import dev.jazzybyte.lite.gateway.filter.core.GatewayRequestBuilder
import org.springframework.http.server.reactive.ServerHttpRequest

/**
 * Spring WebFlux의 ServerHttpRequest를 GatewayRequest 인터페이스에 맞게 래핑하는 어댑터 클래스.
 */
class WebFluxGatewayRequest(val request: ServerHttpRequest) : GatewayRequest {
    override val path: String
        get() = request.path.value()

    override val method: String
        get() = request.method?.name() ?: "GET" // 기본값 설정 또는 예외 처리

    override val headers: Map<String, List<String>>
        get() = request.headers

    override fun mutate(): GatewayRequestBuilder {
        return WebFluxGatewayRequestBuilder(this.request.mutate())
    }
}

/**
 * Spring WebFlux의 ServerHttpRequest.Builder를 GatewayRequestBuilder 인터페이스에 맞게 래핑하는 어댑터 클래스.
 */
class WebFluxGatewayRequestBuilder(private val builder: ServerHttpRequest.Builder) : GatewayRequestBuilder {
    override fun header(name: String, vararg values: String): GatewayRequestBuilder {
        builder.header(name, *values)
        return this
    }

    override fun build(): GatewayRequest {
        return WebFluxGatewayRequest(builder.build())
    }
}
