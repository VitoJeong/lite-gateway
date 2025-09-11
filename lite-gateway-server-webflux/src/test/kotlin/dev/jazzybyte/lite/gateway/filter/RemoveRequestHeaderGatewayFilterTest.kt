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
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("RemoveRequestHeaderGatewayFilter 테스트")
class RemoveRequestHeaderGatewayFilterTest {

    @Test
    @DisplayName("존재하는 요청 헤더가 성공적으로 제거되어야 한다")
    fun shouldRemoveExistingRequestHeaderSuccessfully() {
        // Given
        val headerName = "X-Custom-Header"
        val filter = RemoveRequestHeaderGatewayFilter(headerName)
        
        val mockRequest = mockk<ServerHttpRequest>(relaxed = true)
        val mockRequestBuilder = mockk<ServerHttpRequest.Builder>(relaxed = true)
        val mockNewRequest = mockk<ServerHttpRequest>(relaxed = true)
        val mockExchange = mockk<ServerWebExchange>(relaxed = true)
        val mockExchangeBuilder = mockk<ServerWebExchange.Builder>(relaxed = true)
        val mockNewExchange = mockk<ServerWebExchange>(relaxed = true)
        val mockChain = mockk<GatewayFilterChain>(relaxed = true)
        val mockHeaders = mockk<HttpHeaders>(relaxed = true)
        
        every { mockRequest.headers } returns mockHeaders
        every { mockHeaders.containsKey(headerName) } returns true
        every { mockRequest.mutate() } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns mockNewRequest
        every { mockExchange.request } returns mockRequest
        every { mockExchange.mutate() } returns mockExchangeBuilder
        every { mockExchangeBuilder.request(mockNewRequest) } returns mockExchangeBuilder
        every { mockExchangeBuilder.build() } returns mockNewExchange
        every { mockChain.filter(any()) } returns Mono.empty()
        
        val mockRoute = mockk<Route>(relaxed = true)
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .verifyComplete()
            
        verify { mockChain.filter(any()) }
    }
    
    @Test
    @DisplayName("존재하지 않는 헤더 제거 시 요청이 변경되지 않아야 한다")
    fun shouldNotModifyRequestWhenHeaderDoesNotExist() {
        // Given
        val headerName = "X-Nonexistent-Header"
        val filter = RemoveRequestHeaderGatewayFilter(headerName)
        
        val mockRequest = mockk<ServerHttpRequest>()
        val mockExchange = mockk<ServerWebExchange>()
        val mockChain = mockk<GatewayFilterChain>()
        val mockHeaders = mockk<HttpHeaders>()
        
        every { mockRequest.headers } returns mockHeaders
        every { mockHeaders.containsKey(headerName) } returns false
        every { mockExchange.request } returns mockRequest
        every { mockChain.filter(any()) } returns Mono.empty()
        
        val mockRoute = mockk<Route>()
        val context = WebFluxGatewayContext(mockExchange, mockRoute)
        
        // When
        val result = filter.filter(context, mockChain)
        
        // Then
        StepVerifier.create(result)
            .verifyComplete()
            
        verify { mockChain.filter(any()) }
        // 요청이 변경되지 않았으므로 mutate가 호출되지 않아야 함
        verify(exactly = 0) { mockExchange.mutate() }
    }
    
    @Test
    @DisplayName("빈 헤더 이름으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForBlankHeaderName() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            RemoveRequestHeaderGatewayFilter("")
        }
        assertEquals("Header name cannot be blank", exception.message)
    }
    
    @Test
    @DisplayName("잘못된 헤더 이름으로 필터 생성 시 예외가 발생해야 한다")
    fun shouldThrowExceptionForInvalidHeaderName() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            RemoveRequestHeaderGatewayFilter("invalid header name")
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
            RemoveRequestHeaderGatewayFilter(name)
        }
    }
    
    @Test
    @DisplayName("기본 order 값이 올바르게 설정되어야 한다")
    fun shouldHaveCorrectDefaultOrder() {
        // Given
        val filter = RemoveRequestHeaderGatewayFilter("name")
        
        // When
        val order = filter.order
        
        // Then
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 1100, order)
    }
    
    @Test
    @DisplayName("커스텀 order 값이 올바르게 설정되어야 한다")
    fun shouldHaveCorrectCustomOrder() {
        // Given
        val customOrder = 600
        val filter = RemoveRequestHeaderGatewayFilter("name", customOrder)
        
        // When
        val order = filter.order
        
        // Then
        assertEquals(customOrder, order)
    }
    
    @Test
    @DisplayName("필터 실행 중 예외 발생 시 적절한 오류가 반환되어야 한다")
    fun shouldHandleFilterExecutionException() {
        // Given
        val filter = RemoveRequestHeaderGatewayFilter("name")
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
                error.message!!.contains("Failed to remove request header 'name'") &&
                error.message!!.contains("Test exception")
            }
            .verify()
    }
}