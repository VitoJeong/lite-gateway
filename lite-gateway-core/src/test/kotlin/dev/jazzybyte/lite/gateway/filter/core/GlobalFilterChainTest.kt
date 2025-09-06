package dev.jazzybyte.lite.gateway.filter.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.atomic.AtomicInteger

class GlobalFilterChainTest {

    @Test
    fun `should execute filters in correct order based on order value`() {
        // Given
        val executionOrder = mutableListOf<String>()
        val filter1 = TestOrderedFilter("filter1", 10, executionOrder)
        val filter2 = TestOrderedFilter("filter2", 5, executionOrder)
        val filter3 = TestOrderedFilter("filter3", 15, executionOrder)
        
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            executionOrder.add("finalAction")
            Mono.empty()
        }
        
        val chain = GlobalFilterChain.builder()
            .addGlobalFilter(filter1)
            .addGlobalFilter(filter2)
            .addGlobalFilter(filter3)
            .finalAction(finalAction)
            .build()
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        // Should execute in order: filter2(5), filter1(10), filter3(15), finalAction
        assertEquals(listOf("filter2", "filter1", "filter3", "finalAction"), executionOrder)
    }
    
    @Test
    fun `should prioritize global filters over route filters with same order`() {
        // Given
        val executionOrder = mutableListOf<String>()
        val globalFilter = TestOrderedFilter("global", 10, executionOrder)
        val routeFilter = TestOrderedFilter("route", 10, executionOrder)
        
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            executionOrder.add("finalAction")
            Mono.empty()
        }
        
        val chain = GlobalFilterChain.builder()
            .addRouteFilter(routeFilter)  // Add route filter first
            .addGlobalFilter(globalFilter) // Add global filter second
            .finalAction(finalAction)
            .build()
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        // Global filter should execute before route filter despite being added later
        assertEquals(listOf("global", "route", "finalAction"), executionOrder)
    }
    
    @Test
    fun `should handle mixed global and route filters with different orders`() {
        // Given
        val executionOrder = mutableListOf<String>()
        val globalFilter1 = TestOrderedFilter("global1", 20, executionOrder)
        val globalFilter2 = TestOrderedFilter("global2", 5, executionOrder)
        val routeFilter1 = TestOrderedFilter("route1", 10, executionOrder)
        val routeFilter2 = TestOrderedFilter("route2", 15, executionOrder)
        
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            executionOrder.add("finalAction")
            Mono.empty()
        }
        
        val chain = GlobalFilterChain.builder()
            .addGlobalFilters(listOf(globalFilter1, globalFilter2))
            .addRouteFilters(listOf(routeFilter1, routeFilter2))
            .finalAction(finalAction)
            .build()
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        // Expected order: global2(5), route1(10), route2(15), global1(20)
        assertEquals(listOf("global2", "route1", "route2", "global1", "finalAction"), executionOrder)
    }
    
    @Test
    fun `should handle filters without order (default order 0)`() {
        // Given
        val executionOrder = mutableListOf<String>()
        val orderedFilter = TestOrderedFilter("ordered", 10, executionOrder)
        val unorderedFilter = TestFilter("unordered", executionOrder)
        
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            executionOrder.add("finalAction")
            Mono.empty()
        }
        
        val chain = GlobalFilterChain.builder()
            .addGlobalFilter(orderedFilter)
            .addGlobalFilter(unorderedFilter)
            .finalAction(finalAction)
            .build()
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        // Unordered filter (default order 0) should execute before ordered filter (order 10)
        assertEquals(listOf("unordered", "ordered", "finalAction"), executionOrder)
    }
    
    @Test
    fun `should handle empty filter lists`() {
        // Given
        val finalActionExecuted = AtomicInteger(0)
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            finalActionExecuted.incrementAndGet()
            Mono.empty()
        }
        
        val chain = GlobalFilterChain.builder()
            .finalAction(finalAction)
            .build()
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        assertEquals(1, finalActionExecuted.get())
    }
    
    @Test
    fun `should throw exception when final action is not set`() {
        // Given & When & Then
        assertThrows(IllegalStateException::class.java) {
            GlobalFilterChain.builder()
                .addGlobalFilter(TestFilter("test", mutableListOf()))
                .build()
        }
    }
    
    @Test
    fun `should handle filter exceptions properly`() {
        // Given
        val errorMessage = "Test filter error"
        val errorFilter = ErrorFilter(RuntimeException(errorMessage))
        val finalAction: (GatewayContext) -> Mono<Void> = { _ -> Mono.empty() }
        
        val chain = GlobalFilterChain.builder()
            .addGlobalFilter(errorFilter)
            .finalAction(finalAction)
            .build()
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .expectErrorMatches { it is RuntimeException && it.message == errorMessage }
            .verify()
    }
    
    @Test
    fun `should maintain registration order for filters with same order and type`() {
        // Given
        val executionOrder = mutableListOf<String>()
        val filter1 = TestOrderedFilter("filter1", 10, executionOrder)
        val filter2 = TestOrderedFilter("filter2", 10, executionOrder)
        val filter3 = TestOrderedFilter("filter3", 10, executionOrder)
        
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            executionOrder.add("finalAction")
            Mono.empty()
        }
        
        val chain = GlobalFilterChain.builder()
            .addGlobalFilter(filter1)
            .addGlobalFilter(filter2)
            .addGlobalFilter(filter3)
            .finalAction(finalAction)
            .build()
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        // Should maintain registration order
        assertEquals(listOf("filter1", "filter2", "filter3", "finalAction"), executionOrder)
    }
}

// Test helper classes
private class TestOrderedFilter(
    private val name: String,
    private val order: Int,
    private val executionOrder: MutableList<String>
) : OrderedGatewayFilter {
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        executionOrder.add(name)
        return chain.filter(context)
    }
    
    override fun getOrder(): Int = order
}

private class TestFilter(
    private val name: String,
    private val executionOrder: MutableList<String>
) : GatewayFilter {
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        executionOrder.add(name)
        return chain.filter(context)
    }
}

private class ErrorFilter(
    private val exception: Exception
) : GatewayFilter {
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        return Mono.error(exception)
    }
}

private class TestGatewayContext : GatewayContext {
    override val request: GatewayRequest = TestGatewayRequest()
    override val response: GatewayResponse = TestGatewayResponse()
    private val attributes = mutableMapOf<String, Any>()
    
    override fun getAttribute(name: String): Any? = attributes[name]
    override fun putAttribute(name: String, value: Any) { attributes[name] = value }
    override fun mutate(): GatewayContextBuilder = TestGatewayContextBuilder(this)
}

private class TestGatewayContextBuilder(
    private val original: TestGatewayContext
) : GatewayContextBuilder {
    override fun request(request: GatewayRequest): GatewayContextBuilder = this
    override fun response(response: GatewayResponse): GatewayContextBuilder = this
    override fun attribute(name: String, value: Any): GatewayContextBuilder = this
    override fun build(): GatewayContext = original
}

private class TestGatewayRequest : GatewayRequest {
    override val method: String = "GET"
    override val path: String = "/test"
    override val headers: Map<String, List<String>> = emptyMap()
    override fun mutate(): GatewayRequestBuilder = 
        throw UnsupportedOperationException("Not implemented for test")
}

private class TestGatewayResponse : GatewayResponse {
    override var statusCode: Int = 200
    override fun setHeader(name: String, value: String) {
        // No-op for test
    }
    override fun mutate(): GatewayResponseBuilder = 
        throw UnsupportedOperationException("Not implemented for test")
}