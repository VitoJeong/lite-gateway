package dev.jazzybyte.lite.gateway.handler

import dev.jazzybyte.lite.gateway.handler.GatewayHandlerMapping.Companion.MATCHED_ROUTE_ATTRIBUTE
import dev.jazzybyte.lite.gateway.route.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelOption
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebHandler
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration

private val log = KotlinLogging.logger {}

class FilterHandler : WebHandler {

    private val webClient = WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                // 최대 16MB 메모리 버퍼 제한
                .codecs { config -> config.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
                .build())
        .clientConnector(
            ReactorClientHttpConnector(
                // Netty HttpClient 설정
                HttpClient.create()
                    // 응답 타임아웃 설정
                    .responseTimeout(Duration.ofSeconds(5))
                    // 연결 타임아웃 설정
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30 * 1000)
            )
        )
        .build()

    override fun handle(exchange: ServerWebExchange): Mono<Void> {
        val matchedRoute = exchange.attributes[MATCHED_ROUTE_ATTRIBUTE] as? Route
        // 라우팅 정보가 없으면 요청을 종료시킨다.
        matchedRoute ?: return handleNoRoute(exchange)

        val targetUri = matchedRoute.uri.resolve(exchange.request.uri.path + buildQueryString(exchange.request))
        log.info { "Forwarding request to: $targetUri" }

        return forwardRequest(exchange, targetUri)
    }

    /**
     * 매칭된 라우팅 정보가 없을 경우, 404 Not Found 응답을 반환한다.
     */
    private fun handleNoRoute(exchange: ServerWebExchange): Mono<Void> {
        log.warn { "No matched route found in exchange attributes." }

        val message = "No route matched for request: ${exchange.request.uri}"
        val buffer = exchange.response.bufferFactory()
            .wrap(message.toByteArray(StandardCharsets.UTF_8))

        exchange.response.statusCode = HttpStatus.NOT_FOUND
        exchange.response.headers.contentType = MediaType.TEXT_PLAIN

        return exchange.response.writeWith(Mono.just(buffer))
    }

    private fun forwardRequest(exchange: ServerWebExchange, targetUri: URI): Mono<Void> {
        return webClient.method(exchange.request.method)
            .uri(targetUri)
            .headers { it.addAll(exchange.request.headers) }
            .body(exchange.request.body)
            .exchangeToMono { clientResponse ->
                exchange.response.statusCode = clientResponse.statusCode()
                exchange.response.headers.putAll(clientResponse.headers().asHttpHeaders())
                exchange.response.writeWith(clientResponse.bodyToFlux(DataBuffer::class.java))
            }
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
