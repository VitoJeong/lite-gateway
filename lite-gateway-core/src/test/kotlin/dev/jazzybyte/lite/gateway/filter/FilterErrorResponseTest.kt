package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.exception.FilterExecutionException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("FilterErrorResponse 테스트")
class FilterErrorResponseTest {

    @Test
    @DisplayName("FilterExecutionException으로부터 오류 응답을 생성할 수 있다")
    fun `should create error response from FilterExecutionException`() {
        // given
        val exception = FilterExecutionException(
            message = "Authentication failed",
            filterName = "AuthFilter",
            routeId = "user-service",
            requestId = "req-123"
        )

        // when
        val errorResponse = FilterErrorResponse.fromFilterExecutionException(
            exception = exception,
            status = 401,
            title = "Authentication Error"
        )

        // then
        assertEquals("about:blank", errorResponse.type)
        assertEquals("Authentication Error", errorResponse.title)
        assertEquals(401, errorResponse.status)
        assertEquals(
            "Filter execution failed (filter: 'AuthFilter', route: 'user-service', request: 'req-123'): Authentication failed",
            errorResponse.detail
        )
        assertEquals("AuthFilter", errorResponse.filterName)
        assertEquals("user-service", errorResponse.routeId)
        assertEquals("req-123", errorResponse.requestId)
        assertNotNull(errorResponse.timestamp)
    }

    @Test
    @DisplayName("일반 예외로부터 오류 응답을 생성할 수 있다")
    fun `should create error response from general exception`() {
        // given
        val exception = RuntimeException("Connection timeout")
        val filterName = "CircuitBreakerFilter"
        val routeId = "payment-service"
        val requestId = "req-456"

        // when
        val errorResponse = FilterErrorResponse.fromException(
            filterName = filterName,
            exception = exception,
            status = 503,
            title = "Service Unavailable",
            routeId = routeId,
            requestId = requestId
        )

        // then
        assertEquals("about:blank", errorResponse.type)
        assertEquals("Service Unavailable", errorResponse.title)
        assertEquals(503, errorResponse.status)
        assertEquals("Connection timeout", errorResponse.detail)
        assertEquals(filterName, errorResponse.filterName)
        assertEquals(routeId, errorResponse.routeId)
        assertEquals(requestId, errorResponse.requestId)
        assertNotNull(errorResponse.timestamp)
    }

    @Test
    @DisplayName("메시지가 없는 예외의 경우 기본 메시지를 사용한다")
    fun `should use default message for exception without message`() {
        // given
        val exception = RuntimeException()
        val filterName = "TestFilter"

        // when
        val errorResponse = FilterErrorResponse.fromException(
            filterName = filterName,
            exception = exception
        )

        // then
        assertEquals("Unknown error occurred", errorResponse.detail)
    }

    @Test
    @DisplayName("기본값으로 오류 응답을 생성할 수 있다")
    fun `should create error response with default values`() {
        // given
        val exception = RuntimeException("Test error")
        val filterName = "TestFilter"

        // when
        val errorResponse = FilterErrorResponse.fromException(
            filterName = filterName,
            exception = exception
        )

        // then
        assertEquals("about:blank", errorResponse.type)
        assertEquals("Filter Error", errorResponse.title)
        assertEquals(500, errorResponse.status)
        assertEquals("Test error", errorResponse.detail)
        assertEquals(filterName, errorResponse.filterName)
        assertEquals(null, errorResponse.routeId)
        assertEquals(null, errorResponse.requestId)
        assertNotNull(errorResponse.timestamp)
    }
}