package dev.jazzybyte.lite.gateway.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelOption
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.body
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.net.ConnectException
import java.net.URI
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * WebFluxHttpClient HTTP 요청을 처리하기 위한 클라이언트이다.
 * WebClient를 사용하여 비동기 HTTP 요청을 처리한다.
 */
class WebFluxHttpClient (
    private val maxConnections: Int,
    private val connectionTimeout: Int,
    private val maxHeaderSize: Int,
    private val acquireTimeout: Long,
) {

    private val client: WebClient = create(maxConnections, connectionTimeout, maxHeaderSize, acquireTimeout)

    constructor() : this(
        HttpClientProperties()
    )

    constructor(
        properties: HttpClientProperties
    ) : this(
        maxConnections = properties.maxConnections,
        connectionTimeout = properties.connectionTimeout,
        maxHeaderSize = properties.maxHeaderSize,
        acquireTimeout = properties.acquireTimeout
    )

    companion object {
        /**
         * WebClient 인스턴스를 생성한다.
         *
         * @param properties HTTP 클라이언트 설정 속성
         * @return 생성된 WebClient 인스턴스
         */
        fun create(maxConnections: Int,
                   connectionTimeout: Int,
                   maxHeaderSize: Int,
                   acquireTimeout: Long): WebClient {
            return WebClient.builder()
                .exchangeStrategies(
                    ExchangeStrategies.builder()
                        // 최대 16MB 메모리 버퍼 제한
                        .codecs { config -> config.defaultCodecs()
                            .maxInMemorySize(16 * 1024 * 1024)
                        }
                        .build())
                .clientConnector(
                    ReactorClientHttpConnector(
                        // Netty HttpClient 설정
                        HttpClient.create(// ConnectionProvider로 연결 풀 설정
                            ConnectionProvider.builder("http-pool")
                                .maxConnections(maxConnections) // 최대 연결 수 설정
                                .pendingAcquireMaxCount(1000) // 최대 대기 연결 수
                                .pendingAcquireTimeout(Duration.ofMillis(acquireTimeout))
                                .maxIdleTime(Duration.ofSeconds(60)) // 최대 유휴 시간
                                .build())
                            // 연결 타임아웃 설정
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                            .httpResponseDecoder { spec ->
                                spec.maxHeaderSize(maxHeaderSize) // 최대 헤더 크기 설정
                            }
                    )
                )
                .build()
        }

    }

    fun forwardRequest(exchange: ServerWebExchange, targetUri: URI): Mono<Void> {
        return client.method(exchange.request.method)
            .uri(targetUri)
            .headers { it.addAll(exchange.request.headers) }
            .body(exchange.request.body)
            .exchangeToMono { clientResponse ->
                exchange.response.statusCode = clientResponse.statusCode()
                exchange.response.headers.putAll(clientResponse.headers().asHttpHeaders())
                exchange.response.writeWith(clientResponse.bodyToFlux(DataBuffer::class.java))
            }
            .onErrorResume { ex ->
                when (ex) {
                    is UnknownHostException -> {
                        log.error(ex) { "Unknown host: ${targetUri.host}" }
                        writeTextResponse(exchange, HttpStatus.BAD_GATEWAY, "Unknown host: ${targetUri.host}")
                    }
                    is ConnectException -> {
                        log.error(ex) { "Connection failed to: $targetUri" }
                        writeTextResponse(exchange, HttpStatus.BAD_GATEWAY, "Connection failed to: $targetUri")
                    }
                    is WebClientResponseException -> {
                        log.error(ex) { "Upstream error: ${ex.message}" }
                        writeTextResponse(exchange, ex.statusCode, "Upstream error: ${ex.statusText}")
                    }
                    else -> {
                        log.error(ex) { "Unexpected error while forwarding to: $targetUri" }
                        writeTextResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}")
                    }
                }
            }
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
     * 요청의 쿼리 파라미터를 URL 인코딩하여 쿼리 문자열로 변환한다.
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