package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.filter.core.GatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import kotlin.test.assertTrue

@DisplayName("GatewayFilterChainFactory 테스트")
class GatewayFilterChainFactoryTest {

    private val mockFilters = listOf<GatewayFilter>(mockk(), mockk())
    private val mockFinalAction: (GatewayContext) -> Mono<Void> = { Mono.empty() }

    @Test
    @DisplayName("오류 처리 기능이 포함된 필터 체인을 생성할 수 있다")
    fun `should create error handling filter chain`() {
        // when
        val chain = GatewayFilterChainFactory.createErrorHandlingChain(
            filters = mockFilters,
            finalAction = mockFinalAction,
            routeId = "test-route",
            requestId = "req-123"
        )
        
        // then
        assertTrue(chain is ErrorHandlingGatewayFilterChain)
    }

    @Test
    @DisplayName("기본 필터 체인을 생성할 수 있다")
    fun `should create default filter chain`() {
        // when
        val chain = GatewayFilterChainFactory.createDefaultChain(
            filters = mockFilters,
            finalAction = mockFinalAction
        )
        
        // then
        assertTrue(chain is DefaultGatewayFilterChain)
    }

    @Test
    @DisplayName("오류 처리 활성화 시 ErrorHandlingGatewayFilterChain을 생성한다")
    fun `should create ErrorHandlingGatewayFilterChain when error handling is enabled`() {
        // when
        val chain = GatewayFilterChainFactory.createChain(
            filters = mockFilters,
            finalAction = mockFinalAction,
            enableErrorHandling = true,
            routeId = "test-route",
            requestId = "req-123"
        )
        
        // then
        assertTrue(chain is ErrorHandlingGatewayFilterChain)
    }

    @Test
    @DisplayName("오류 처리 비활성화 시 DefaultGatewayFilterChain을 생성한다")
    fun `should create DefaultGatewayFilterChain when error handling is disabled`() {
        // when
        val chain = GatewayFilterChainFactory.createChain(
            filters = mockFilters,
            finalAction = mockFinalAction,
            enableErrorHandling = false
        )
        
        // then
        assertTrue(chain is DefaultGatewayFilterChain)
    }

    @Test
    @DisplayName("기본적으로 오류 처리가 활성화된 체인을 생성한다")
    fun `should create error handling chain by default`() {
        // when
        val chain = GatewayFilterChainFactory.createChain(
            filters = mockFilters,
            finalAction = mockFinalAction
        )
        
        // then
        assertTrue(chain is ErrorHandlingGatewayFilterChain)
    }
}