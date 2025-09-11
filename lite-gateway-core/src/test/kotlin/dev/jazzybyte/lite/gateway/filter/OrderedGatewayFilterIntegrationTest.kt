package dev.jazzybyte.lite.gateway.filter

import org.junit.jupiter.api.Test
import org.springframework.core.Ordered
import reactor.core.publisher.Mono
import kotlin.test.assertEquals

class OrderedGatewayFilterIntegrationTest {

    @Test
    fun `filters should be sortable by order for execution chain`() {
        // Given
        val highPriorityFilter = TestOrderedFilter("high", Ordered.HIGHEST_PRECEDENCE)
        val mediumPriorityFilter = TestOrderedFilter("medium", 0)
        val lowPriorityFilter = TestOrderedFilter("low", Ordered.LOWEST_PRECEDENCE)

        val unsortedFilters = listOf(lowPriorityFilter, highPriorityFilter, mediumPriorityFilter)

        // When
        val sortedFilters = unsortedFilters.sortedBy { it.order }

        // Then
        assertEquals("high", sortedFilters[0].name)
        assertEquals("medium", sortedFilters[1].name)
        assertEquals("low", sortedFilters[2].name)
    }

    @Test
    fun `OrderedGatewayFilter should work with Spring Ordered interface methods`() {
        // Given
        val filter = TestOrderedFilter("test", 100)

        // When & Then
        assertEquals(100, filter.order)
        assertEquals(100, (filter as Ordered).order)
    }

    /**
     * 테스트용 OrderedGatewayFilter 구현체
     */
    private class TestOrderedFilter(
        val name: String,
        private val orderValue: Int
    ) : OrderedGatewayFilter {

        override fun getOrder(): Int = orderValue

        override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
            // 실제 필터 로직은 여기에 구현
            return chain.filter(context)
        }
    }
}