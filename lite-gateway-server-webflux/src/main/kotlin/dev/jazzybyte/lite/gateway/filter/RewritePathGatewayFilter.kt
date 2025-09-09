package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import dev.jazzybyte.lite.gateway.filter.core.GatewayContext
import io.github.oshai.kotlinlogging.KotlinLogging
import reactor.core.publisher.Mono
import java.util.regex.Pattern

/**
 * 정규식을 사용하여 요청 경로를 리라이트하는 필터
 * 
 * @param regexp 매칭할 정규식 패턴
 * @param replacement 치환할 문자열 (그룹 참조 지원: $1, $2 등)
 */
class RewritePathGatewayFilter(
    private val regexp: String,
    private val replacement: String
) : GatewayFilter {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val pattern: Pattern

    init {
        require(regexp.isNotBlank()) { "Regular expression cannot be blank" }
        require(replacement.isNotBlank()) { "Replacement string cannot be blank" }

        try {
            pattern = Pattern.compile(regexp)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid regular expression: $regexp", e)
        }
    }

    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        val webfluxContext = context as WebFluxGatewayContext
        val request = webfluxContext.exchange.request
        val originalPath = request.path.pathWithinApplication().value()

        log.debug { "Original path: $originalPath, Pattern: $regexp, Replacement: $replacement" }

        val matcher = pattern.matcher(originalPath)
        
        return if (matcher.matches()) {
            val newPath = matcher.replaceAll(replacement)
            log.debug { "Path rewritten from '$originalPath' to '$newPath'" }

            val mutatedRequest = request.mutate()
                .path(newPath)
                .build()

            val mutatedExchange = webfluxContext.exchange.mutate()
                .request(mutatedRequest)
                .build()

            webfluxContext.exchange = mutatedExchange

            chain.filter(context)
        } else {
            log.debug { "Path '$originalPath' does not match pattern '$regexp', no rewrite performed" }
            chain.filter(context)
        }
    }
}