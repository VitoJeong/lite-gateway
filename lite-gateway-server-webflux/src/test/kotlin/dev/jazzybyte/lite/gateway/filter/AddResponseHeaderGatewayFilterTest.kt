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
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("AddResponseHeaderGatewayFilter 테스트")
class AddResponseHeaderGatewayFilterTest {

    @Test
    @DisplayName("응답 헤더가 성공적으로 추가되어야 한다")
    fun shouldAddResponseHeaderSuccessfully() {
        // Given
        val headerName = "X-Custom-Header"
        val headerValue = "test-value"
        val filter = AddResponseHeaderGatewayFilter(headerName, headerValue)
        
        val mockExchange = mockk<ServerWebExchange>(relaxed = true)
        val mockResponse = mockk<ServerHttpResponse>(relaxed = true)
        val mockHeaders = mockk<HttpHeaders>(relaxed = true)
        val mockChain = mockk<GatewayFilterChain>(relaxed = true)
        
        every { mockExchange.response } returns mockResponse
        every { mockResponse.headers } returns mockHeaders
        every { mockChain.filter(any()) } returns Mono.empty()
        
        val mockRoute = mockk<Route>(relaxed = true)
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .verifyComplete()
            
        verify { mockChain.filter(any()) }
        verify { mockHeaders.add(headerName, headerValue) }
    }
    
    @Test
    @DisplayName("빈 헤더 이름으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForBlankHeaderName() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            AddResponseHeaderGatewayFilter("", "value")
        }
        assertEquals("Header name cannot be blank", exception.message)
    }
    
    @Test
    @DisplayName("빈 헤더 값으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForBlankHeaderValue() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            AddResponseHeaderGatewayFilter("name", "")
        }
        assertEquals("Header value cannot be blank", exception.message)
    }
    
    @Test
    @DisplayName("잘못된 헤더 이름으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForInvalidHeaderName() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            AddResponseHeaderGatewayFilter("invalid header name", "value")
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
            "Cache-Control",
            "Set-Cookie",
            "Access-Control-Allow-Origin",
            "X-API-Version"
        )
        
        // When & Then
        validNames.forEach { name ->
            // Should not throw exception
            AddResponseHeaderGatewayFilter(name, "value")
        }
    }
    
    @Test
    @DisplayName("기본 order 값이 올바르게 설정되어야 한다")
    fun shouldHaveCorrectDefaultOrder() {
        // Given
        val filter = AddResponseHeaderGatewayFilter("name", "value")
        
        // When
        val order = filter.order
        
        // Then
        assertEquals(Ordered.LOWEST_PRECEDENCE - 1000, order)
    }
    
    @Test
    @DisplayName("커스텀 order 값이 올바르게 설정되어야 한다")
    fun shouldHaveCorrectCustomOrder() {
        // Given
        val customOrder = 500
        val filter = AddResponseHeaderGatewayFilter("name", "value", customOrder)
        
        // When
        val order = filter.order
        
        // Then
        assertEquals(customOrder, order)
    }
    
    @Test
    @DisplayName("체인 필터 실행 실패 시 적절한 오류가 전파되어야 한다")
    fun shouldPropagateChainFilterError() {
        // Given
        val filter = AddResponseHeaderGatewayFilter("name", "value")
        val mockExchange = mockk<ServerWebExchange>(relaxed = true)
        val mockChain = mockk<GatewayFilterChain>(relaxed = true)
        
        every { mockChain.filter(any()) } returns Mono.error(RuntimeException("Chain error"))
        
        val mockRoute = mockk<Route>(relaxed = true)
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is RuntimeException && error.message == "Chain error"
            }
            .verify()
    }
    
    @Test
    @DisplayName("Post-filter 로직에서 예외 발생 시 적절한 오류가 반환되어야 한다")
    fun shouldHandlePostFilterException() {
        // Given
        val filter = AddResponseHeaderGatewayFilter("name", "value")
        val mockExchange = mockk<ServerWebExchange>(relaxed = true)
        val mockResponse = mockk<ServerHttpResponse>(relaxed = true)
        val mockHeaders = mockk<HttpHeaders>(relaxed = true)
        val mockChain = mockk<GatewayFilterChain>(relaxed = true)
        
        every { mockExchange.response } returns mockResponse
        every { mockResponse.headers } returns mockHeaders
        every { mockHeaders.add(any(), any()) } throws RuntimeException("Header add failed")
        every { mockChain.filter(any()) } returns Mono.empty()
        
        val mockRoute = mockk<Route>(relaxed = true)
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is RuntimeException && error.message == "Header add failed"
            }
            .verify()
    }
}