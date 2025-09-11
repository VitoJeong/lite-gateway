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
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@DisplayName("RewritePathGatewayFilter 테스트")
class RewritePathGatewayFilterTest {

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
        @DisplayName("유효한 정규식과 치환 문자열로 필터를 생성할 수 있다")
        fun `creates filter with valid regexp and replacement`() {
            // When & Then
            val filter = RewritePathGatewayFilter("/api/v1/(.*)", "/v1/$1")
            assertThat(filter).isNotNull
        }

        @Test
        @DisplayName("빈 정규식으로 필터 생성 시 예외가 발생한다")
        fun `throws exception when regexp is blank`() {
            // When & Then
            assertThatThrownBy { RewritePathGatewayFilter("", "/v1/$1") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Regular expression cannot be blank")
        }

        @Test
        @DisplayName("빈 치환 문자열로 필터 생성 시 예외가 발생한다")
        fun `throws exception when replacement is blank`() {
            // When & Then
            assertThatThrownBy { RewritePathGatewayFilter("/api/v1/(.*)", "") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Replacement string cannot be blank")
        }

        @Test
        @DisplayName("잘못된 정규식으로 필터 생성 시 예외가 발생한다")
        fun `throws exception when regexp is invalid`() {
            // When & Then
            assertThatThrownBy { RewritePathGatewayFilter("[invalid", "/v1/$1") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Invalid regular expression")
        }
    }

    @Nested
    @DisplayName("경로 리라이트")
    inner class PathRewrite {

        @Test
        @DisplayName("정규식에 매칭되는 경로를 올바르게 리라이트한다")
        fun `rewrites path when it matches the pattern`() {
            // Given
            val filter = RewritePathGatewayFilter("/api/v1/(.*)", "/v1/$1")
            val originalPath = "/api/v1/users"
            val expectedPath = "/v1/users"

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
        @DisplayName("정규식에 매칭되지 않는 경로는 리라이트하지 않는다")
        fun `does not rewrite path when it does not match the pattern`() {
            // Given
            val filter = RewritePathGatewayFilter("/api/v1/(.*)", "/v1/$1")
            val originalPath = "/api/v2/users"

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
        @DisplayName("복잡한 정규식 패턴으로 경로를 리라이트한다")
        fun `rewrites path with complex regex pattern`() {
            // Given
            val filter = RewritePathGatewayFilter("/api/v([0-9]+)/([^/]+)/(.*)", "/version/$1/$2/$3")
            val originalPath = "/api/v2/users/123"
            val expectedPath = "/version/2/users/123"

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
        @DisplayName("루트 경로를 리라이트한다")
        fun `rewrites root path`() {
            // Given
            val filter = RewritePathGatewayFilter("^/$", "/health")
            val originalPath = "/"
            val expectedPath = "/health"

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