package dev.jazzybyte.lite.gateway.route

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class InMemoryRouteLocatorTest {

    @Test
    fun `should match multiple routes`() {
        val route1 = Route.builder()
            .id("r1")
            .predicate(RoutePredicate { it.request.uri.path.startsWith("/api") })
            .uri("http://api-1.dev")
            .build()

        val route2 = Route.builder()
            .id("r2")
            .predicate(RoutePredicate { it.request.uri.path.contains("users") })
            .uri("http://api-1.dev")
            .build()
        val locator = InMemoryRouteLocator(listOf(route1, route2))
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://api-1.dev/api/users").build())

        val matched = locator.locate(exchange).block()

        assertNotNull(matched)
        assertThat(matched!!.id).isEqualTo("r1")
    }

    @Test
    fun `should return null if no route matches`() {
        val route = Route.builder()
            .id("no-match")
            .predicate { ex -> ex.request.uri.path.startsWith("/admin") }
            .uri("http://admin")
            .build()
        val locator = InMemoryRouteLocator(listOf(route))
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/user").build())

        assertTrue(locator.locate(exchange).blockOptional().isEmpty)
    }
}