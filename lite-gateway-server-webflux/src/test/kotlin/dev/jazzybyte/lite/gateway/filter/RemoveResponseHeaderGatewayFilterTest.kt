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

@DisplayName("RemoveResponseHeaderGatewayFilter 테스트")
class RemoveResponseHeaderGatewayFilterTest {

    @Test
    @DisplayName("존재하는 응답 헤더가 성공적으로 제거되어야 한다")
    fun shouldRemoveExistingResponseHeaderSuccessfully() {
        // Given
        val headerName = "X-Custom-Header"
        val filter = RemoveResponseHeaderGatewayFilter(headerName)
        
        val mockExchange = mockk<ServerWebExchange>(relaxed = true)
        val mockResponse = mockk<ServerHttpResponse>(relaxed = true)
        val mockHeaders = mockk<HttpHeaders>(relaxed = true)
        val mockChain = mockk<GatewayFilterChain>(relaxed = true)
        
        every { mockExchange.response } returns mockResponse
        every { mockResponse.headers } returns mockHeaders
        every { mockHeaders.containsKey(headerName) } returns true
        every { mockChain.filter(any()) } returns Mono.empty()
        
        val mockRoute = mockk<Route>(relaxed = true)
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .verifyComplete()
            
        verify { mockChain.filter(any()) }
        verify { mockHeaders.remove(headerName) }
    }
    
    @Test
    @DisplayName("존재하지 않는 헤더 제거 시 응답이 변경되지 않아야 한다")
    fun shouldNotModifyResponseWhenHeaderDoesNotExist() {
        // Given
        val headerName = "X-Nonexistent-Header"
        val filter = RemoveResponseHeaderGatewayFilter(headerName)
        
        val mockExchange = mockk<ServerWebExchange>(relaxed = true)
        val mockResponse = mockk<ServerHttpResponse>(relaxed = true)
        val mockHeaders = mockk<HttpHeaders>(relaxed = true)
        val mockChain = mockk<GatewayFilterChain>(relaxed = true)
        
        every { mockExchange.response } returns mockResponse
        every { mockResponse.headers } returns mockHeaders
        every { mockHeaders.containsKey(headerName) } returns false
        every { mockChain.filter(any()) } returns Mono.empty()
        
        val mockRoute = mockk<Route>(relaxed = true)
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .verifyComplete()
            
        verify { mockChain.filter(any()) }
        // 헤더가 존재하지 않으므로 remove가 호출되지 않아야 함
        verify(exactly = 0) { mockHeaders.remove(headerName) }
    }
    
    @Test
    @DisplayName("빈 헤더 이름으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForBlankHeaderName() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            RemoveResponseHeaderGatewayFilter("")
        }
        assertEquals("Header name cannot be blank", exception.message)
    }
    
    @Test
    @DisplayName("잘못된 헤더 이름으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForInvalidHeaderName() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            RemoveResponseHeaderGatewayFilter("invalid header name")
        }
        assertTrue(exception.message!!.contains("Invalid header name"))
    }
    
    @Test
    @DisplayName("유효한 헤더 이름들이 허용되어야 한다")
    fun shouldAllowValidHeaderNames() {
        // Given
        val validNames = listOf(
            "X-Custom-Header",
            "Server",
            "X-Powered-By",
            "X-Frame-Options",
            "Strict-Transport-Security",
            "X-Content-Type-Options"
        )
        
        // When & Then
        validNames.forEach { name ->
            // Should not throw exception
            RemoveResponseHeaderGatewayFilter(name)
        }
    }
    
    @Test
    @DisplayName("기본 order 값이 올바르게 설정되어야 한다")
    fun shouldHaveCorrectDefaultOrder() {
        // Given
        val filter = RemoveResponseHeaderGatewayFilter("name")
        
        // When
        val order = filter.order
        
        // Then
        assertEquals(Ordered.LOWEST_PRECEDENCE - 900, order)
    }
    
    @Test
    @DisplayName("커스텀 order 값이 올바르게 설정되어야 한다")
    fun shouldHaveCorrectCustomOrder() {
        // Given
        val customOrder = 600
        val filter = RemoveResponseHeaderGatewayFilter("name", customOrder)
        
        // When
        val order = filter.order
        
        // Then
        assertEquals(customOrder, order)
    }
    
    @Test
    @DisplayName("체인 필터 실행 실패 시 적절한 오류가 전파되어야 한다")
    fun shouldPropagateChainFilterError() {
        // Given
        val filter = RemoveResponseHeaderGatewayFilter("name")
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
        val filter = RemoveResponseHeaderGatewayFilter("name")
        val mockExchange = mockk<ServerWebExchange>(relaxed = true)
        val mockResponse = mockk<ServerHttpResponse>(relaxed = true)
        val mockHeaders = mockk<HttpHeaders>(relaxed = true)
        val mockChain = mockk<GatewayFilterChain>(relaxed = true)
        
        every { mockExchange.response } returns mockResponse
        every { mockResponse.headers } returns mockHeaders
        every { mockHeaders.containsKey(any()) } returns true
        every { mockHeaders.remove(any()) } throws RuntimeException("Header remove failed")
        every { mockChain.filter(any()) } returns Mono.empty()
        
        val mockRoute = mockk<Route>(relaxed = true)
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is RuntimeException && error.message == "Header remove failed"
            }
            .verify()
    }
}