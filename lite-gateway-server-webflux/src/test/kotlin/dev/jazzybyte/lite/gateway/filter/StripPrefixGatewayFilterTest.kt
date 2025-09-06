package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DisplayName("StripPrefixGatewayFilter 테스트")
class StripPrefixGatewayFilterTest {

    private lateinit var context: WebFluxGatewayContext
    private lateinit var chain: GatewayFilterChain
    private lateinit var exchange: ServerWebExchange
    private lateinit var request: ServerHttpRequest

    @BeforeEach
    fun setUp() {
        context = mockk<WebFluxGatewayContext>()
        chain = mockk<GatewayFilterChain>()
        exchange = mockk<ServerWebExchange>()
        request = mockk<ServerHttpRequest>()

        every { context.exchange } returns exchange
        every { exchange.request } returns request
        every { chain.filter(any()) } returns Mono.empty()
    }

    @Nested
    @DisplayName("필터 생성")
    inner class FilterCreation {

        @Test
        @DisplayName("유효한 parts 값으로 필터를 생성할 수 있다")
        fun `creates filter with valid parts`() {
            // When & Then
            val filter = StripPrefixGatewayFilter(2)
            assertThat(filter).isNotNull
        }

        @Test
        @DisplayName("0 이하의 parts 값으로 필터 생성 시 예외가 발생한다")
        fun `throws exception when parts is zero or negative`() {
            // When & Then
            assertThatThrownBy { StripPrefixGatewayFilter(0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Parts must be greater than 0")

            assertThatThrownBy { StripPrefixGatewayFilter(-1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Parts must be greater than 0")
        }

        @Test
        @DisplayName("10을 초과하는 parts 값으로 필터 생성 시 예외가 발생한다")
        fun `throws exception when parts exceeds safety limit`() {
            // When & Then
            assertThatThrownBy { StripPrefixGatewayFilter(11) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Parts must be less than or equal to 10")
        }
    }

    @Nested
    @DisplayName("접두어 제거")
    inner class PrefixStripping {

        @Test
        @DisplayName("지정된 개수의 경로 세그먼트를 제거한다")
        fun `strips specified number of path segments`() {
            // Given
            val filter = StripPrefixGatewayFilter(2)
            val originalPath = "/api/v1/users/123"
            val expectedPath = "/users/123"

            val pathContainer = mockk<org.springframework.http.server.PathContainer>()
            every { pathContainer.value() } returns originalPath
            every { request.path.pathWithinApplication() } returns pathContainer

            val requestBuilder = mockk<ServerHttpRequest.Builder>()
            val mutatedRequest = mockk<ServerHttpRequest>()
            every { request.mutate() } returns requestBuilder
            every { requestBuilder.path(expectedPath) } returns requestBuilder
            every { requestBuilder.build() } returns mutatedRequest

            val exchangeBuilder = mockk<ServerWebExchange.Builder>()
            val mutatedExchange = mockk<ServerWebExchange>()
            every { exchange.mutate() } returns exchangeBuilder
            every { exchangeBuilder.request(mutatedRequest) } returns exchangeBuilder
            every { exchangeBuilder.build() } returns mutatedExchange

            every { context.exchange = mutatedExchange } returns Unit

            // When
            val result = filter.filter(context, chain)

            // Then
            StepVerifier.create(result)
                .verifyComplete()

            verify { context.exchange = mutatedExchange }
            verify { chain.filter(context) }
        }

        @Test
        @DisplayName("모든 세그먼트를 제거하면 루트 경로를 반환한다")
        fun `returns root path when all segments are stripped`() {
            // Given
            val filter = StripPrefixGatewayFilter(3)
            val originalPath = "/api/v1/users"
            val expectedPath = "/"

            val pathContainer = mockk<org.springframework.http.server.PathContainer>()
            every { pathContainer.value() } returns originalPath
            every { request.path.pathWithinApplication() } returns pathContainer

            val requestBuilder = mockk<ServerHttpRequest.Builder>()
            val mutatedRequest = mockk<ServerHttpRequest>()
            every { request.mutate() } returns requestBuilder
            every { requestBuilder.path(expectedPath) } returns requestBuilder
            every { requestBuilder.build() } returns mutatedRequest

            val exchangeBuilder = mockk<ServerWebExchange.Builder>()
            val mutatedExchange = mockk<ServerWebExchange>()
            every { exchange.mutate() } returns exchangeBuilder
            every { exchangeBuilder.request(mutatedRequest) } returns exchangeBuilder
            every { exchangeBuilder.build() } returns mutatedExchange

            every { context.exchange = mutatedExchange } returns Unit

            // When
            val result = filter.filter(context, chain)

            // Then
            StepVerifier.create(result)
                .verifyComplete()

            verify { context.exchange = mutatedExchange }
            verify { chain.filter(context) }
        }

        @Test
        @DisplayName("제거할 세그먼트보다 적은 경로에서는 루트 경로를 반환한다")
        fun `returns root path when path has fewer segments than parts`() {
            // Given
            val filter = StripPrefixGatewayFilter(5)
            val originalPath = "/api/v1"
            val expectedPath = "/"

            val pathContainer = mockk<org.springframework.http.server.PathContainer>()
            every { pathContainer.value() } returns originalPath
            every { request.path.pathWithinApplication() } returns pathContainer

            val requestBuilder = mockk<ServerHttpRequest.Builder>()
            val mutatedRequest = mockk<ServerHttpRequest>()
            every { request.mutate() } returns requestBuilder
            every { requestBuilder.path(expectedPath) } returns requestBuilder
            every { requestBuilder.build() } returns mutatedRequest

            val exchangeBuilder = mockk<ServerWebExchange.Builder>()
            val mutatedExchange = mockk<ServerWebExchange>()
            every { exchange.mutate() } returns exchangeBuilder
            every { exchangeBuilder.request(mutatedRequest) } returns exchangeBuilder
            every { exchangeBuilder.build() } returns mutatedExchange

            every { context.exchange = mutatedExchange } returns Unit

            // When
            val result = filter.filter(context, chain)

            // Then
            StepVerifier.create(result)
                .verifyComplete()

            verify { context.exchange = mutatedExchange }
            verify { chain.filter(context) }
        }

        @Test
        @DisplayName("루트 경로는 변경하지 않는다")
        fun `does not modify root path`() {
            // Given
            val filter = StripPrefixGatewayFilter(1)
            val originalPath = "/"

            val pathContainer = mockk<org.springframework.http.server.PathContainer>()
            every { pathContainer.value() } returns originalPath
            every { request.path.pathWithinApplication() } returns pathContainer

            // When
            val result = filter.filter(context, chain)

            // Then
            StepVerifier.create(result)
                .verifyComplete()

            verify(exactly = 0) { request.mutate() }
            verify { chain.filter(context) }
        }

        @Test
        @DisplayName("단일 세그먼트 경로를 올바르게 처리한다")
        fun `handles single segment path correctly`() {
            // Given
            val filter = StripPrefixGatewayFilter(1)
            val originalPath = "/api"
            val expectedPath = "/"

            val pathContainer = mockk<org.springframework.http.server.PathContainer>()
            every { pathContainer.value() } returns originalPath
            every { request.path.pathWithinApplication() } returns pathContainer

            val requestBuilder = mockk<ServerHttpRequest.Builder>()
            val mutatedRequest = mockk<ServerHttpRequest>()
            every { request.mutate() } returns requestBuilder
            every { requestBuilder.path(expectedPath) } returns requestBuilder
            every { requestBuilder.build() } returns mutatedRequest

            val exchangeBuilder = mockk<ServerWebExchange.Builder>()
            val mutatedExchange = mockk<ServerWebExchange>()
            every { exchange.mutate() } returns exchangeBuilder
            every { exchangeBuilder.request(mutatedRequest) } returns exchangeBuilder
            every { exchangeBuilder.build() } returns mutatedExchange

            every { context.exchange = mutatedExchange } returns Unit

            // When
            val result = filter.filter(context, chain)

            // Then
            StepVerifier.create(result)
                .verifyComplete()

            verify { context.exchange = mutatedExchange }
            verify { chain.filter(context) }
        }
    }
}