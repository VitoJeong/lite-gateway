package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.filter.core.GatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import dev.jazzybyte.lite.gateway.filter.core.GatewayRequest
import dev.jazzybyte.lite.gateway.filter.core.GatewayResponse
import dev.jazzybyte.lite.gateway.filter.core.GatewayContextBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class DefaultGatewayFilterChainTest {

    @Test
    fun `should execute filters in correct order`() {
        // Given
        val executionOrder = mutableListOf<String>()
        val filter1 = TestFilter("filter1", executionOrder)
        val filter2 = TestFilter("filter2", executionOrder)
        val filter3 = TestFilter("filter3", executionOrder)
        
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            executionOrder.add("finalAction")
            Mono.empty()
        }
        
        val chain = DefaultGatewayFilterChain(
            filters = listOf(filter1, filter2, filter3),
            finalAction = finalAction
        )
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        assertEquals(listOf("filter1", "filter2", "filter3", "finalAction"), executionOrder)
    }
    
    @Test
    fun `should be thread safe with concurrent executions`() {
        // Given
        val executionCount = AtomicInteger(0)
        val filter = CountingFilter(executionCount)
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            executionCount.incrementAndGet()
            Mono.empty()
        }
        
        val chain = DefaultGatewayFilterChain(
            filters = listOf(filter),
            finalAction = finalAction
        )
        
        val numberOfThreads = 10
        val numberOfExecutionsPerThread = 100
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val latch = CountDownLatch(numberOfThreads)
        val errors = AtomicReference<Exception>()
        
        // When
        repeat(numberOfThreads) {
            executor.submit {
                try {
                    repeat(numberOfExecutionsPerThread) {
                        val context = TestGatewayContext()
                        StepVerifier.create(chain.filter(context))
                            .verifyComplete()
                    }
                } catch (e: Exception) {
                    errors.set(e)
                } finally {
                    latch.countDown()
                }
            }
        }
        
        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Test should complete within 10 seconds")
        assertNull(errors.get(), "No exceptions should occur during concurrent execution")
        
        // Each execution should increment the counter twice (filter + finalAction)
        val expectedCount = numberOfThreads * numberOfExecutionsPerThread * 2
        assertEquals(expectedCount, executionCount.get())
        
        executor.shutdown()
    }
    
    @Test
    fun `should handle empty filter list`() {
        // Given
        val finalActionExecuted = AtomicInteger(0)
        val finalAction: (GatewayContext) -> Mono<Void> = { _ ->
            finalActionExecuted.incrementAndGet()
            Mono.empty()
        }
        
        val chain = DefaultGatewayFilterChain(
            filters = emptyList(),
            finalAction = finalAction
        )
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        assertEquals(1, finalActionExecuted.get())
    }
    
    @Test
    fun `should handle filter exceptions properly`() {
        // Given
        val errorMessage = "Test filter error"
        val errorFilter = ErrorFilter(RuntimeException(errorMessage))
        val finalAction: (GatewayContext) -> Mono<Void> = { _ -> Mono.empty() }
        
        val chain = DefaultGatewayFilterChain(
            filters = listOf(errorFilter),
            finalAction = finalAction
        )
        
        val context = TestGatewayContext()
        
        // When & Then
        StepVerifier.create(chain.filter(context))
            .expectErrorMatches { it is RuntimeException && it.message == errorMessage }
            .verify()
    }
    
    @Test
    fun `should create independent chain instances for each filter execution`() {
        // Given
        val chainInstances = mutableSetOf<DefaultGatewayFilterChain>()
        val filter = ChainCapturingFilter(chainInstances)
        val finalAction: (GatewayContext) -> Mono<Void> = { _ -> Mono.empty() }
        
        val chain = DefaultGatewayFilterChain(
            filters = listOf(filter, filter), // Same filter twice to capture multiple chain instances
            finalAction = finalAction
        )
        
        val context = TestGatewayContext()
        
        // When
        StepVerifier.create(chain.filter(context))
            .verifyComplete()
        
        // Then
        // Should have 2 different chain instances: one for each filter execution
        // The original chain (index 0) passes a new chain (index 1) to the first filter
        // The first filter's chain (index 1) passes a new chain (index 2) to the second filter
        assertEquals(2, chainInstances.size)
    }
}

// Test helper classes
private class TestFilter(
    private val name: String,
    private val executionOrder: MutableList<String>
) : GatewayFilter {
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        executionOrder.add(name)
        return chain.filter(context)
    }
}

private class CountingFilter(
    private val counter: AtomicInteger
) : GatewayFilter {
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        counter.incrementAndGet()
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

private class ChainCapturingFilter(
    private val chainInstances: MutableSet<DefaultGatewayFilterChain>
) : GatewayFilter {
    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        if (chain is DefaultGatewayFilterChain) {
            chainInstances.add(chain)
        }
        return chain.filter(context)
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
    override fun mutate(): dev.jazzybyte.lite.gateway.filter.core.GatewayRequestBuilder = 
        throw UnsupportedOperationException("Not implemented for test")
}

private class TestGatewayResponse : GatewayResponse {
    override var statusCode: Int = 200
    override fun setHeader(name: String, value: String) {
        // No-op for test
    }
    override fun mutate(): dev.jazzybyte.lite.gateway.filter.core.GatewayResponseBuilder = 
        throw UnsupportedOperationException("Not implemented for test")
}