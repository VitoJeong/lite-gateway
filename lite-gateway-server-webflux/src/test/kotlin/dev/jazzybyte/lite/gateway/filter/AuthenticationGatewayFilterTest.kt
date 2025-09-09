package dev.jazzybyte.lite.gateway.filter

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("AuthenticationGatewayFilter 테스트")
class AuthenticationGatewayFilterTest {

    @Test
    @DisplayName("Critical 필터로서 적절한 실패 상태 코드를 반환한다")
    fun `should return appropriate failure status code as critical filter`() {
        // given
        val filter = AuthenticationGatewayFilter()
        
        // when
        val statusCode = filter.getFailureStatusCode()
        
        // then
        assertEquals(401, statusCode)
    }

    @Test
    @DisplayName("Critical 필터로서 적절한 실패 메시지를 반환한다")
    fun `should return appropriate failure message as critical filter`() {
        // given
        val filter = AuthenticationGatewayFilter()
        val cause = RuntimeException("Invalid token")
        
        // when
        val message = filter.getFailureMessage(cause)
        
        // then
        assertEquals("Authentication failed", message)
    }

    @Test
    @DisplayName("필터 순서를 설정할 수 있다")
    fun `should allow setting filter order`() {
        // given
        val customOrder = 50
        val filter = AuthenticationGatewayFilter(customOrder)
        
        // when
        val order = filter.getOrder()
        
        // then
        assertEquals(customOrder, order)
    }

    @Test
    @DisplayName("기본 필터 순서는 100이다")
    fun `should have default order of 100`() {
        // given
        val filter = AuthenticationGatewayFilter()
        
        // when
        val order = filter.getOrder()
        
        // then
        assertEquals(100, order)
    }
}