package dev.jazzybyte.lite.gateway.filter.core

import org.junit.jupiter.api.Test
import org.springframework.core.Ordered
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderedGatewayFilterTest {

    @Test
    fun `OrderedGatewayFilter should extend both GatewayFilter and Ordered`() {
        // Given
        val filter = TestOrderedGatewayFilter(100)
        
        // Then
        assertTrue(filter is GatewayFilter)
        assertTrue(filter is Ordered)
        assertTrue(filter is OrderedGatewayFilter)
    }

    @Test
    fun `OrderedGatewayFilter should return correct order value`() {
        // Given
        val expectedOrder = 42
        val filter = TestOrderedGatewayFilter(expectedOrder)
        
        // When
        val actualOrder = filter.order
        
        // Then
        assertEquals(expectedOrder, actualOrder)
    }

    @Test
    fun `OrderedGatewayFilter should be sortable by order`() {
        // Given
        val filter1 = TestOrderedGatewayFilter(100)
        val filter2 = TestOrderedGatewayFilter(50)
        val filter3 = TestOrderedGatewayFilter(200)
        val filters = listOf(filter1, filter2, filter3)
        
        // When
        val sortedFilters = filters.sortedBy { it.order }
        
        // Then
        assertEquals(50, sortedFilters[0].order)
        assertEquals(100, sortedFilters[1].order)
        assertEquals(200, sortedFilters[2].order)
    }

    @Test
    fun `OrderedGatewayFilter should work with Spring Ordered constants`() {
        // Given
        val highestPrecedenceFilter = TestOrderedGatewayFilter(Ordered.HIGHEST_PRECEDENCE)
        val lowestPrecedenceFilter = TestOrderedGatewayFilter(Ordered.LOWEST_PRECEDENCE)
        
        // Then
        assertEquals(Ordered.HIGHEST_PRECEDENCE, highestPrecedenceFilter.order)
        assertEquals(Ordered.LOWEST_PRECEDENCE, lowestPrecedenceFilter.order)
        assertTrue(highestPrecedenceFilter.order < lowestPrecedenceFilter.order)
    }

    /**
     * 테스트용 OrderedGatewayFilter 구현체
     */
    private class TestOrderedGatewayFilter(private val orderValue: Int) : OrderedGatewayFilter {
        
        override fun getOrder(): Int = orderValue
        
        override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
            return chain.filter(context)
        }
    }
}