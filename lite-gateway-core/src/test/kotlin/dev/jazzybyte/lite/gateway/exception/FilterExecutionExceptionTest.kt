package dev.jazzybyte.lite.gateway.exception

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@DisplayName("FilterExecutionException 테스트")
class FilterExecutionExceptionTest {

    @Test
    @DisplayName("필터 이름만으로 예외를 생성할 수 있다")
    fun `should create exception with filter name only`() {
        // given
        val message = "Filter execution failed"
        val filterName = "AddRequestHeaderFilter"
        
        // when
        val exception = FilterExecutionException(message, filterName)
        
        // then
        assertEquals("Filter execution failed (filter: 'AddRequestHeaderFilter'): Filter execution failed", exception.message)
        assertEquals(filterName, exception.filterName)
        assertNull(exception.routeId)
        assertNull(exception.requestId)
        assertNull(exception.cause)
    }

    @Test
    @DisplayName("모든 컨텍스트 정보와 함께 예외를 생성할 수 있다")
    fun `should create exception with all context information`() {
        // given
        val message = "Timeout occurred"
        val filterName = "CircuitBreakerFilter"
        val routeId = "user-service"
        val requestId = "req-123"
        val cause = RuntimeException("Connection timeout")
        
        // when
        val exception = FilterExecutionException(message, filterName, routeId, requestId, cause)
        
        // then
        assertEquals(
            "Filter execution failed (filter: 'CircuitBreakerFilter', route: 'user-service', request: 'req-123'): Timeout occurred",
            exception.message
        )
        assertEquals(filterName, exception.filterName)
        assertEquals(routeId, exception.routeId)
        assertEquals(requestId, exception.requestId)
        assertSame(cause, exception.cause)
    }

    @Test
    @DisplayName("라우트 ID만 있는 경우 적절한 메시지를 생성한다")
    fun `should create appropriate message with route id only`() {
        // given
        val message = "Authentication failed"
        val filterName = "AuthFilter"
        val routeId = "protected-api"
        
        // when
        val exception = FilterExecutionException(message, filterName, routeId)
        
        // then
        assertEquals(
            "Filter execution failed (filter: 'AuthFilter', route: 'protected-api'): Authentication failed",
            exception.message
        )
        assertEquals(filterName, exception.filterName)
        assertEquals(routeId, exception.routeId)
        assertNull(exception.requestId)
    }

    @Test
    @DisplayName("요청 ID만 있는 경우 적절한 메시지를 생성한다")
    fun `should create appropriate message with request id only`() {
        // given
        val message = "Rate limit exceeded"
        val filterName = "RateLimitFilter"
        val requestId = "req-456"
        
        // when
        val exception = FilterExecutionException(message, filterName, null, requestId)
        
        // then
        assertEquals(
            "Filter execution failed (filter: 'RateLimitFilter', request: 'req-456'): Rate limit exceeded",
            exception.message
        )
        assertEquals(filterName, exception.filterName)
        assertNull(exception.routeId)
        assertEquals(requestId, exception.requestId)
    }
}