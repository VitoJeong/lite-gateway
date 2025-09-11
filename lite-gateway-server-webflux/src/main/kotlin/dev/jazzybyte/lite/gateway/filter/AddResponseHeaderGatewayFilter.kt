package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import org.springframework.core.Ordered
import reactor.core.publisher.Mono

/**
 * 응답 헤더를 추가하는 게이트웨이 필터.
 * 
 * 이 필터는 나가는 HTTP 응답에 지정된 헤더를 추가한다.
 * Post-filter 로직으로 동작하여 프록시 응답 후에 헤더를 추가한다.
 * Reactive Chain 패턴을 적용하여 chain.filter() 완료 후 헤더를 추가한다.
 * 
 * @param name 추가할 헤더의 이름
 * @param value 추가할 헤더의 값
 * @param order 필터 실행 순서 (기본값: LOWEST_PRECEDENCE - 1000)
 */
class AddResponseHeaderGatewayFilter(
    private val name: String, 
    private val value: String,
    private val order: Int = Ordered.LOWEST_PRECEDENCE - 1000
) : OrderedGatewayFilter {
    
    init {
        // 매개변수 검증
        require(name.isNotBlank()) { "Header name cannot be blank" }
        require(value.isNotBlank()) { "Header value cannot be blank" }
        require(isValidHeaderName(name)) { "Invalid header name: $name" }
    }
    
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        return try {
            // Post-filter 로직: chain.filter() 완료 후 응답 헤더 추가
            chain.filter(context).then(Mono.fromRunnable {
                val webfluxContext = context as WebFluxGatewayContext
                val response = webfluxContext.exchange.response
                response.headers.add(name, value)
            })
        } catch (e: Exception) {
            Mono.error(RuntimeException("Failed to add response header '$name': ${e.message}", e))
        }
    }
    
    override fun getOrder(): Int = order
    
    /**
     * HTTP 헤더 이름의 유효성을 검증한다.
     * RFC 7230에 따라 헤더 이름은 토큰 문자만 포함해야 한다.
     */
    private fun isValidHeaderName(name: String): Boolean {
        return name.matches(Regex("^[!#$%&'*+\\-.0-9A-Z^_`a-z|~]+$"))
    }
}
