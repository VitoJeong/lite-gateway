package dev.jazzybyte.lite.gateway.handler

import dev.jazzybyte.lite.gateway.client.WebFluxHttpClient
import dev.jazzybyte.lite.gateway.handler.GatewayHandlerMapping.Companion.MATCHED_ROUTE_ATTRIBUTE
import dev.jazzybyte.lite.gateway.route.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebHandler
import reactor.core.publisher.Mono
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

class FilterHandler(
    private val webclient: WebFluxHttpClient
) : WebHandler {

    override fun handle(exchange: ServerWebExchange): Mono<Void> {
        val matchedRoute = exchange.attributes[MATCHED_ROUTE_ATTRIBUTE] as? Route
        matchedRoute ?: return handleNoRoute(exchange)

        val targetUri = matchedRoute.uri.resolve(
            exchange.request.uri.path + buildQueryString(exchange.request)
        )
        log.info { "Forwarding request to: $targetUri" }

        return webclient.forwardRequest(exchange, targetUri)
    }

    private fun handleNoRoute(exchange: ServerWebExchange): Mono<Void> {
        log.warn { "No matched route found in exchange attributes." }

        val message = "No route matched for request: ${exchange.request.uri}"
        return writeTextResponse(exchange, HttpStatus.NOT_FOUND, message)
    }

    /**
     * 텍스트 기반 응답을 작성하는 유틸리티 메서드
     */
    private fun writeTextResponse(
        exchange: ServerWebExchange,
        status: HttpStatusCode,
        message: String
    ): Mono<Void> {
        val buffer = exchange.response.bufferFactory()
            .wrap(message.toByteArray(StandardCharsets.UTF_8))
        exchange.response.statusCode = status
        exchange.response.headers.contentType = MediaType.TEXT_PLAIN
        return exchange.response.writeWith(Mono.just(buffer))
    }

    /**
     * 요청의 쿼리 파라미터를 URL 인코딩하여 쿼리 문자열로 변환합니다.
     */
    private fun buildQueryString(request: ServerHttpRequest): String {
        val queryParams = request.queryParams

        if (queryParams.isEmpty()) {
            return ""
        } else {
            val sb = StringBuilder("?")
            queryParams.forEach { (key, values) ->
                values.forEach { value ->
                    sb.append("${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}&")
                }
            }
            return sb.removeSuffix("&").toString()
        }
    }
}
