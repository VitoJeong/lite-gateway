package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.InetSocketAddress

@DisplayName("RequestRateLimiterGatewayFilter 테스트")
class RequestRateLimiterGatewayFilterTest {

    private lateinit var context: WebFluxGatewayContext
    private lateinit var chain: GatewayFilterChain
    private lateinit var exchange: ServerWebExchange
    private lateinit var request: ServerHttpRequest
    private lateinit var response: ServerHttpResponse

    @BeforeEach
    fun setUp() {
        context = mockk<WebFluxGatewayContext>()
        chain = mockk<GatewayFilterChain>()
        exchange = mockk<ServerWebExchange>()
        request = mockk<ServerHttpRequest>()
        response = mockk<ServerHttpResponse>()

        every { context.exchange } returns exchange
        every { exchange.request } returns request
        every { exchange.response } returns response
        every { chain.filter(any()) } returns Mono.empty()
        every { response.setComplete() } returns Mono.empty()
    }

    @Nested
    @DisplayName("필터 생성")
    inner class FilterCreation {

        @Test
        @DisplayName("유효한 매개변수로 필터를 생성할 수 있다")
        fun `creates filter with valid parameters`() {
            // When & Then
            val filter = RequestRateLimiterGatewayFilter(10.0, 20L, 1L)
            assertThat(filter).isNotNull
        }

        @Test
        @DisplayName("잘못된 replenishRate로 필터 생성 시 예외가 발생한다")
        fun `throws exception when replenishRate is invalid`() {
            // When & Then
            assertThatThrownBy { RequestRateLimiterGatewayFilter(0.0, 20L, 1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Replenish rate must be greater than 0")
        }

        @Test
        @DisplayName("잘못된 burstCapacity로 필터 생성 시 예외가 발생한다")
        fun `throws exception when burstCapacity is invalid`() {
            // When & Then
            assertThatThrownBy { RequestRateLimiterGatewayFilter(10.0, 0L, 1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Burst capacity must be greater than 0")
        }
    }

    @Nested
    @DisplayName("속도 제한")
    inner class RateLimiting {

        @Test
        @DisplayName("제한 내의 요청은 통과시킨다")
        fun `allows requests within rate limit`() {
            // Given
            val filter = RequestRateLimiterGatewayFilter(10.0, 20L, 1L)
            setupMockRequest("192.168.1.1")

            // When
            val result = filter.filter(context, chain)

            // Then
            StepVerifier.create(result)
                .verifyComplete()

            verify { chain.filter(context) }
        }

        @Test
        @DisplayName("제한을 초과한 요청은 429 응답을 반환한다")
        fun `returns 429 for requests exceeding rate limit`() {
            // Given
            val filter = RequestRateLimiterGatewayFilter(1.0, 1L, 1L)
            setupMockRequest("192.168.1.2")
            setupMockResponse()

            // When - 첫 번째 요청은 성공
            val firstResult = filter.filter(context, chain)
            StepVerifier.create(firstResult).verifyComplete()

            // 두 번째 요청은 실패해야 함
            val secondResult = filter.filter(context, chain)

            // Then
            StepVerifier.create(secondResult)
                .verifyComplete()

            verify { response.statusCode = HttpStatus.TOO_MANY_REQUESTS }
        }
    }

    private fun setupMockRequest(clientIp: String) {
        val headers = mockk<HttpHeaders>()
        every { request.headers } returns headers
        every { headers.getFirst("X-Forwarded-For") } returns null
        every { headers.getFirst("X-Real-IP") } returns null
        every { request.remoteAddress } returns InetSocketAddress(clientIp, 8080)
    }

    private fun setupMockResponse() {
        val headers = mockk<HttpHeaders>()
        every { response.headers } returns headers
        every { response.setStatusCode(any()) } returns true
        every { headers.add(any(), any()) } returns Unit
    }
}