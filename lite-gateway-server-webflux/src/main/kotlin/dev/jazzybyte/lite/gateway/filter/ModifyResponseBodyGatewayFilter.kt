package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

/**
 * 응답 본문을 변환하는 필터
 * 
 * Post-filter 로직으로 구현되어 프록시 응답을 받은 후 본문을 가공한다.
 * Reactive Chain 패턴을 적용하여 chain.filter() 호출 후 응답 본문을 변환한다.
 * 
 * @param transformFunction 본문 변환 함수 (원본 문자열 -> 변환된 문자열)
 * @param contentType 변환 후 Content-Type (선택적, null이면 원본 유지)
 * @param order 필터 실행 순서 (기본값: 0)
 */
class ModifyResponseBodyGatewayFilter(
    private val transformFunction: (String) -> String,
    private val contentType: MediaType? = null,
    private val order: Int = 0
) : OrderedGatewayFilter {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getOrder(): Int = order

    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        val webfluxContext = context as WebFluxGatewayContext
        val originalResponse = webfluxContext.exchange.response

        log.debug { "ModifyResponseBodyGatewayFilter processing response for ${webfluxContext.exchange.request.path}" }

        // 응답을 가로채기 위한 데코레이터 생성
        val decoratedResponse = object : ServerHttpResponseDecorator(originalResponse) {
            override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                return DataBufferUtils.join(Flux.from(body))
                    .flatMap { dataBuffer ->
                        try {
                            // 원본 응답 본문을 문자열로 변환
                            val bytes = ByteArray(dataBuffer.readableByteCount())
                            dataBuffer.read(bytes)
                            val originalBody = String(bytes, StandardCharsets.UTF_8)
                            log.debug { "Original response body length: ${originalBody.length}" }

                            // 변환 함수 적용
                            val transformedBody = transformFunction(originalBody)
                            log.debug { "Transformed response body length: ${transformedBody.length}" }

                            // Content-Type 헤더 업데이트 (지정된 경우)
                            contentType?.let { newContentType ->
                                delegate.headers.contentType = newContentType
                                log.debug { "Updated Content-Type to: $newContentType" }
                            }

                            // Content-Length 헤더 업데이트
                            val transformedBytes = transformedBody.toByteArray(StandardCharsets.UTF_8)
                            delegate.headers.contentLength = transformedBytes.size.toLong()

                            // 변환된 본문으로 새 DataBuffer 생성
                            val bufferFactory: DataBufferFactory = delegate.bufferFactory()
                            val newBuffer = bufferFactory.wrap(transformedBytes)

                            // 원본 버퍼 해제
                            DataBufferUtils.release(dataBuffer)

                            // 변환된 응답 본문 작성
                            super.writeWith(Flux.just(newBuffer))
                        } catch (e: Exception) {
                            log.error(e) { "Failed to transform response body" }
                            // 변환 실패 시 원본 응답 반환
                            DataBufferUtils.release(dataBuffer)
                            super.writeWith(Flux.just(dataBuffer))
                        }
                    }
                    .onErrorResume { error ->
                        log.error(error) { "Error during response body transformation" }
                        // 오류 발생 시 원본 응답 체인으로 fallback
                        super.writeWith(Flux.from(body))
                    }
            }
        }

        // 데코레이터된 응답으로 exchange 수정
        val modifiedExchange = webfluxContext.exchange.mutate()
            .response(decoratedResponse)
            .build()

        // 수정된 exchange로 컨텍스트 업데이트
        webfluxContext.exchange = modifiedExchange

        // 체인 실행 (프록시 호출 등)
        return chain.filter(context)
    }
}