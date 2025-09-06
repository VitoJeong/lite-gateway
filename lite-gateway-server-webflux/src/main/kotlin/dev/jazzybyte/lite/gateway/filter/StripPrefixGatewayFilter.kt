package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import dev.jazzybyte.lite.gateway.filter.core.GatewayContext
import io.github.oshai.kotlinlogging.KotlinLogging
import reactor.core.publisher.Mono

/**
 * 요청 경로에서 지정된 개수의 접두어 세그먼트를 제거하는 필터
 * 
 * @param parts 제거할 경로 세그먼트 수
 */
class StripPrefixGatewayFilter(
    private val parts: Int
) : GatewayFilter {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        require(parts > 0) { "Parts must be greater than 0, but was: $parts" }
        require(parts <= 10) { "Parts must be less than or equal to 10 for safety, but was: $parts" }
    }

    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        val webfluxContext = context as WebFluxGatewayContext
        val request = webfluxContext.exchange.request
        val originalPath = request.path.pathWithinApplication().value()

        log.debug { "Original path: $originalPath, Parts to strip: $parts" }

        val newPath = stripPrefix(originalPath, parts)
        
        return if (newPath != originalPath) {
            log.debug { "Path stripped from '$originalPath' to '$newPath'" }

            val mutatedRequest = request.mutate()
                .path(newPath)
                .build()

            val mutatedExchange = webfluxContext.exchange.mutate()
                .request(mutatedRequest)
                .build()

            webfluxContext.exchange = mutatedExchange

            chain.filter(context)
        } else {
            log.debug { "No prefix to strip from path '$originalPath'" }
            chain.filter(context)
        }
    }

    /**
     * 경로에서 지정된 개수의 접두어 세그먼트를 제거한다
     */
    private fun stripPrefix(path: String, parts: Int): String {
        if (path == "/" || parts <= 0) {
            return path
        }

        val segments = path.split("/").filter { it.isNotEmpty() }
        
        return if (segments.size <= parts) {
            // 모든 세그먼트를 제거하면 루트 경로 반환
            "/"
        } else {
            // 지정된 개수만큼 세그먼트 제거
            val remainingSegments = segments.drop(parts)
            "/" + remainingSegments.joinToString("/")
        }
    }
}