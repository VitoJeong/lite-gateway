package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import dev.jazzybyte.lite.gateway.route.Route
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("AddRequestHeaderGatewayFilter 테스트")
class AddRequestHeaderGatewayFilterTest {

    @Test
    @DisplayName("요청 헤더가 성공적으로 추가되어야 한다")
    fun shouldAddRequestHeaderSuccessfully() {
        // Given
        val headerName = "X-Custom-Header"
        val headerValue = "test-value"
        val filter = AddRequestHeaderGatewayFilter(headerName, headerValue)
        
        val mockRequest = mockk<ServerHttpRequest>()
        val mockRequestBuilder = mockk<ServerHttpRequest.Builder>()
        val mockNewRequest = mockk<ServerHttpRequest>()
        val mockExchange = mockk<ServerWebExchange>()
        val mockExchangeBuilder = mockk<ServerWebExchange.Builder>()
        val mockNewExchange = mockk<ServerWebExchange>()
        val mockChain = mockk<GatewayFilterChain>()
        
        every { mockRequest.mutate() } returns mockRequestBuilder
        every { mockRequestBuilder.header(headerName, headerValue) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockNewRequest
        every { mockExchange.request } returns mockRequest
        every { mockExchange.mutate() } returns mockExchangeBuilder
        every { mockExchangeBuilder.request(mockNewRequest) } returns mockExchangeBuilder
        every { mockExchangeBuilder.build() } returns mockNewExchange
        every { mockChain.filter(any()) } returns Mono.empty()
        
        val mockRoute = mockk<Route>()
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .verifyComplete()
            
        verify { mockRequestBuilder.header(headerName, headerValue) }
        verify { mockChain.filter(any()) }
        assertEquals(mockNewExchange, context.exchange)
    }
    
    @Test
    @DisplayName("빈 헤더 이름으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForBlankHeaderName() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            AddRequestHeaderGatewayFilter("", "value")
        }
        assertEquals("Header name cannot be blank", exception.message)
    }
    
    @Test
    @DisplayName("빈 헤더 값으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForBlankHeaderValue() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            AddRequestHeaderGatewayFilter("name", "")
        }
        assertEquals("Header value cannot be blank", exception.message)
    }
    
    @Test
    @DisplayName("잘못된 헤더 이름으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForInvalidHeaderName() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            AddRequestHeaderGatewayFilter("invalid header name", "value")
        }
        assertTrue(exception.message!!.contains("Invalid header name"))
    }
    
    @Test
    @DisplayName("유효한 헤더 이름들이 허용되어야 한다")
    fun shouldAllowValidHeaderNames() {
        // Given
        val validNames = listOf(
            "X-Custom-Header",
            "Content-Type",
            "Authorization",
            "User-Agent",
            "Accept",
            "X-API-Key"
        )
        
        // When & Then
        validNames.forEach { name ->
            // Should not throw exception
            AddRequestHeaderGatewayFilter(name, "value")
        }
    }
    
    @Test
    @DisplayName("기본 order 값이 올바르게 설정되어야 한다")
    fun shouldHaveCorrectDefaultOrder() {
        // Given
        val filter = AddRequestHeaderGatewayFilter("name", "value")
        
        // When
        val order = filter.order
        
        // Then
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 1000, order)
    }
    
    @Test
    @DisplayName("커스텀 order 값이 올바르게 설정되어야 한다")
    fun shouldHaveCorrectCustomOrder() {
        // Given
        val customOrder = 500
        val filter = AddRequestHeaderGatewayFilter("name", "value", customOrder)
        
        // When
        val order = filter.order
        
        // Then
        assertEquals(customOrder, order)
    }
    
    @Test
    @DisplayName("필터 실행 중 예외 발생 시 적절한 오류가 반환되어야 한다")
    fun shouldHandleFilterExecutionException() {
        // Given
        val filter = AddRequestHeaderGatewayFilter("name", "value")
        val mockExchange = mockk<ServerWebExchange>()
        val mockChain = mockk<GatewayFilterChain>()
        
        every { mockExchange.request } throws RuntimeException("Test exception")
        
        val mockRoute = mockk<Route>()
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is RuntimeException && 
                error.message!!.contains("Failed to add request header 'name'") &&
                error.message!!.contains("Test exception")
            }
            .verify()
    }
}