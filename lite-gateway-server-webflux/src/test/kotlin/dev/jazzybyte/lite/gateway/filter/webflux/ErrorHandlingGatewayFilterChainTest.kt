package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.exception.FilterExecutionException
import dev.jazzybyte.lite.gateway.filter.CriticalFilter
import dev.jazzybyte.lite.gateway.filter.GatewayContext
import dev.jazzybyte.lite.gateway.filter.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.GatewayFilterChain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

@DisplayName("ErrorHandlingGatewayFilterChain 테스트")
class ErrorHandlingGatewayFilterChainTest {

    private val mockContext = mockk<GatewayContext>()
    private val mockFinalAction: (GatewayContext) -> Mono<Void> = mockk()

    @Test
    @DisplayName("정상적인 필터 실행 시 다음 필터로 진행한다")
    fun `should proceed to next filter on successful execution`() {
        // given
        val filter1 = mockk<GatewayFilter>()
        val filter2 = mockk<GatewayFilter>()
        
        every { filter1.filter(any(), any()) } answers {
            val chain = secondArg<GatewayFilterChain>()
            chain.filter(firstArg())
        }
        every { filter2.filter(any(), any()) } answers {
            val chain = secondArg<GatewayFilterChain>()
            chain.filter(firstArg())
        }
        every { mockFinalAction(any()) } returns Mono.empty()
        
        val chain = ErrorHandlingGatewayFilterChain(
            filters = listOf(filter1, filter2),
            finalAction = mockFinalAction
        )
        
        // when & then
        StepVerifier.create(chain.filter(mockContext))
            .expectComplete()
            .verify(Duration.ofSeconds(1))
        
        verify { filter1.filter(mockContext, any()) }
        verify { filter2.filter(mockContext, any()) }
        verify { mockFinalAction(mockContext) }
    }

    @Test
    @DisplayName("Non-Critical 필터 실패 시 다음 필터를 계속 실행한다")
    fun `should continue to next filter when non-critical filter fails`() {
        // given
        val failingFilter = mockk<GatewayFilter>()
        val successFilter = mockk<GatewayFilter>()
        
        every { failingFilter.filter(any(), any()) } returns Mono.error(RuntimeException("Non-critical error"))
        every { successFilter.filter(any(), any()) } answers {
            val chain = secondArg<GatewayFilterChain>()
            chain.filter(firstArg())
        }
        every { mockFinalAction(any()) } returns Mono.empty()
        
        val chain = ErrorHandlingGatewayFilterChain(
            filters = listOf(failingFilter, successFilter),
            finalAction = mockFinalAction
        )
        
        // when & then
        StepVerifier.create(chain.filter(mockContext))
            .expectComplete()
            .verify(Duration.ofSeconds(1))
        
        verify { failingFilter.filter(mockContext, any()) }
        verify { successFilter.filter(mockContext, any()) }
        verify { mockFinalAction(mockContext) }
    }

    @Test
    @DisplayName("Critical 필터 실패 시 필터 체인을 중단한다")
    fun `should stop filter chain when critical filter fails`() {
        // given
        val criticalGatewayFilter = object : GatewayFilter, CriticalFilter {
            override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
                return Mono.error(RuntimeException("Critical error"))
            }
            
            override fun getFailureStatusCode(): Int = 401
            override fun getFailureMessage(cause: Throwable): String = "Authentication failed"
        }
        
        val nextFilter = mockk<GatewayFilter>()
        every { nextFilter.filter(any(), any()) } returns Mono.empty()
        every { mockFinalAction(any()) } returns Mono.empty()
        
        val chain = ErrorHandlingGatewayFilterChain(
            filters = listOf(criticalGatewayFilter, nextFilter),
            finalAction = mockFinalAction
        )
        
        // when & then
        StepVerifier.create(chain.filter(mockContext))
            .expectComplete()
            .verify(Duration.ofSeconds(1))
        
        // nextFilter와 finalAction은 실행되지 않아야 함
        verify(exactly = 0) { nextFilter.filter(any(), any()) }
        verify(exactly = 0) { mockFinalAction(any()) }
    }

    @Test
    @DisplayName("FilterExecutionException을 적절히 처리한다")
    fun `should handle FilterExecutionException properly`() {
        // given
        val filterExecutionException = FilterExecutionException(
            message = "Filter execution failed",
            filterName = "TestFilter",
            routeId = "test-route",
            requestId = "req-123"
        )
        
        val failingFilter = mockk<GatewayFilter>()
        every { failingFilter.filter(any(), any()) } returns Mono.error(filterExecutionException)
        every { mockFinalAction(any()) } returns Mono.empty()
        
        val chain = ErrorHandlingGatewayFilterChain(
            filters = listOf(failingFilter),
            finalAction = mockFinalAction,
            routeId = "test-route",
            requestId = "req-123"
        )
        
        // when & then
        StepVerifier.create(chain.filter(mockContext))
            .expectComplete()
            .verify(Duration.ofSeconds(1))
        
        verify { failingFilter.filter(mockContext, any()) }
        // Non-critical filter이므로 최종 액션이 실행되어야 함
        verify { mockFinalAction(mockContext) }
    }

    @Test
    @DisplayName("빈 필터 목록의 경우 바로 최종 액션을 실행한다")
    fun `should execute final action immediately when filter list is empty`() {
        // given
        every { mockFinalAction(any()) } returns Mono.empty()
        
        val chain = ErrorHandlingGatewayFilterChain(
            filters = emptyList(),
            finalAction = mockFinalAction
        )
        
        // when & then
        StepVerifier.create(chain.filter(mockContext))
            .expectComplete()
            .verify(Duration.ofSeconds(1))
        
        verify { mockFinalAction(mockContext) }
    }

    @Test
    @DisplayName("최종 액션 실행 실패를 적절히 처리한다")
    fun `should handle final action execution failure properly`() {
        // given
        every { mockFinalAction(any()) } returns Mono.error(RuntimeException("Final action failed"))
        
        val chain = ErrorHandlingGatewayFilterChain(
            filters = emptyList(),
            finalAction = mockFinalAction
        )
        
        // when & then
        StepVerifier.create(chain.filter(mockContext))
            .expectError(RuntimeException::class.java)
            .verify(Duration.ofSeconds(1))
        
        verify { mockFinalAction(mockContext) }
    }
}