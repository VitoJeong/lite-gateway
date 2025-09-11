package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import reactor.core.publisher.Mono

/**
 * 요청 본문을 변환하는 필터 (단순화된 구현)
 * 
 * @param transformFunction 본문 변환 함수
 * @param contentType 변환 후 Content-Type (선택적)
 */
class ModifyRequestBodyGatewayFilter(
    private val transformFunction: (String) -> String,
    private val contentType: MediaType? = null
) : GatewayFilter {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        val webfluxContext = context as WebFluxGatewayContext
        val request = webfluxContext.exchange.request

        log.debug { "ModifyRequestBodyGatewayFilter processing request for ${request.method} ${request.path}" }

        // GET, DELETE 등 본문이 없는 요청은 그대로 통과
        val methodName = request.method?.toString()
        if (methodName in listOf("GET", "DELETE", "HEAD", "OPTIONS")) {
            log.debug { "Skipping body modification for $methodName request" }
            return chain.filter(context)
        }

        // 실제 본문 변환 로직은 복잡하므로 현재는 헤더만 수정
        if (contentType != null) {
            val modifiedRequest = request.mutate()
                .headers { headers ->
                    headers.contentType = contentType
                }
                .build()

            val modifiedExchange = webfluxContext.exchange.mutate()
                .request(modifiedRequest)
                .build()

            webfluxContext.exchange = modifiedExchange
        }

        return chain.filter(context)
    }
}