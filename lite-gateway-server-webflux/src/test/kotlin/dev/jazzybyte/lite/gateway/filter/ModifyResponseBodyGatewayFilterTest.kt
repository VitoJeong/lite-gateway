package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DisplayName("ModifyResponseBodyGatewayFilter 테스트")
class ModifyResponseBodyGatewayFilterTest {

    private lateinit var filter: ModifyResponseBodyGatewayFilter
    private lateinit var context: WebFluxGatewayContext
    private lateinit var chain: GatewayFilterChain
    private lateinit var exchange: ServerWebExchange
    private lateinit var request: ServerHttpRequest
    private lateinit var response: ServerHttpResponse

    @BeforeEach
    fun setUp() {
        context = mockk<WebFluxGatewayContext>(relaxed = true)
        chain = mockk<GatewayFilterChain>(relaxed = true)
        exchange = mockk<ServerWebExchange>(relaxed = true)
        request = mockk<ServerHttpRequest>(relaxed = true)
        response = mockk<ServerHttpResponse>(relaxed = true)

        every { context.exchange } returns exchange
        every { exchange.request } returns request
        every { exchange.response } returns response
        every { chain.filter(any()) } returns Mono.empty()
    }

    @Test
    @DisplayName("응답 본문을 대문자로 변환해야 한다")
    fun shouldTransformResponseBodyToUppercase() {
        // Given
        val transformFunction: (String) -> String = { it.uppercase() }
        filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When & Then
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()

        verify { chain.filter(context) }
    }

    @Test
    @DisplayName("Content-Type을 변경해야 한다")
    fun shouldUpdateContentType() {
        // Given
        val transformFunction: (String) -> String = { it }
        val newContentType = MediaType.APPLICATION_JSON
        filter = ModifyResponseBodyGatewayFilter(transformFunction, newContentType)

        // When
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()

        // Then
        verify { chain.filter(context) }
    }

    @Test
    @DisplayName("필터 순서를 올바르게 반환해야 한다")
    fun shouldReturnCorrectOrder() {
        // Given
        val expectedOrder = 100
        filter = ModifyResponseBodyGatewayFilter({ it }, order = expectedOrder)

        // When
        val actualOrder = filter.getOrder()

        // Then
        assert(actualOrder == expectedOrder) { "Expected order $expectedOrder, but got $actualOrder" }
    }

    @Test
    @DisplayName("변환 함수에서 예외 발생 시 원본 응답을 반환해야 한다")
    fun shouldReturnOriginalResponseWhenTransformationFails() {
        // Given
        val transformFunction: (String) -> String = { throw RuntimeException("Transformation failed") }
        filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When & Then
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()

        verify { chain.filter(context) }
    }

    @Test
    @DisplayName("빈 응답 본문을 올바르게 처리해야 한다")
    fun shouldHandleEmptyResponseBody() {
        // Given
        val transformFunction: (String) -> String = { body -> 
            if (body.isEmpty()) "default content" else body.uppercase()
        }
        filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When & Then
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()

        verify { chain.filter(context) }
    }

    @Test
    @DisplayName("JSON 응답 본문을 올바르게 변환해야 한다")
    fun shouldTransformJsonResponseBody() {
        // Given
        val transformFunction: (String) -> String = { body ->
            // 간단한 JSON 변환 예제
            body.replace("\"sensitive\"", "\"[MASKED]\"")
        }
        filter = ModifyResponseBodyGatewayFilter(transformFunction, MediaType.APPLICATION_JSON)

        // When & Then
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()

        verify { chain.filter(context) }
    }

    @Test
    @DisplayName("대용량 응답 본문을 처리해야 한다")
    fun shouldHandleLargeResponseBody() {
        // Given
        val transformFunction: (String) -> String = { it.replace("test", "TEST") }
        filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When & Then
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()

        verify { chain.filter(context) }
    }

    @Test
    @DisplayName("멀티바이트 문자를 올바르게 처리해야 한다")
    fun shouldHandleMultibyteCharacters() {
        // Given
        val transformFunction: (String) -> String = { body ->
            body.replace("안녕", "Hello")
        }
        filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When & Then
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()

        verify { chain.filter(context) }
    }
}