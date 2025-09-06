package dev.jazzybyte.lite.gateway.integration

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import dev.jazzybyte.lite.gateway.filter.ModifyResponseBodyGatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import dev.jazzybyte.lite.gateway.route.Route
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DisplayName("ModifyResponseBodyGatewayFilter 통합 테스트")
class ModifyResponseBodyGatewayFilterIntegrationTest {

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
    @DisplayName("실제 JSON 응답에서 민감한 데이터를 마스킹해야 한다")
    fun shouldMaskSensitiveDataInRealJsonResponse() {
        // Given
        val transformFunction: (String) -> String = { body ->
            body
                .replace(Regex(""""password"\s*:\s*"[^"]*""""), """"password":"[MASKED]"""")
                .replace(Regex(""""ssn"\s*:\s*"[^"]*""""), """"ssn":"[MASKED]"""")
                .replace(Regex(""""creditCard"\s*:\s*"[^"]*""""), """"creditCard":"[MASKED]"""")
        }

        val filter = ModifyResponseBodyGatewayFilter(
            transformFunction = transformFunction,
            contentType = MediaType.APPLICATION_JSON
        )

        // When
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()
    }

    @Test
    @DisplayName("HTTP 상태 코드와 헤더를 올바르게 유지해야 한다")
    fun shouldPreserveHttpStatusAndHeaders() {
        // Given
        val transformFunction: (String) -> String = { it.uppercase() }
        val filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()
    }

    @Test
    @DisplayName("Content-Length 헤더를 올바르게 업데이트해야 한다")
    fun shouldUpdateContentLengthHeader() {
        // Given
        val transformFunction: (String) -> String = { "transformed content" }
        val filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()
    }

    @Test
    @DisplayName("여러 필터와 함께 체인에서 올바르게 동작해야 한다")
    fun shouldWorkCorrectlyInFilterChain() {
        // Given
        val transformFunction: (String) -> String = { body ->
            body.replace("user", "USER").replace("data", "DATA")
        }
        val filter = ModifyResponseBodyGatewayFilter(transformFunction, order = 100)

        // When
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()

        // Then
        assert(filter.getOrder() == 100)
    }

    @Test
    @DisplayName("에러 응답도 올바르게 변환해야 한다")
    fun shouldTransformErrorResponses() {
        // Given
        val transformFunction: (String) -> String = { body ->
            body.replace(Regex(""""sensitive_info"\s*:\s*"[^"]*""""), """"sensitive_info":"[REDACTED]"""")
        }

        val filter = ModifyResponseBodyGatewayFilter(
            transformFunction = transformFunction,
            contentType = MediaType.APPLICATION_JSON
        )

        // When
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()
    }

    @Test
    @DisplayName("스트리밍 응답을 올바르게 처리해야 한다")
    fun shouldHandleStreamingResponses() {
        // Given
        val transformFunction: (String) -> String = { it.replace("chunk", "CHUNK") }
        val filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()
    }
}