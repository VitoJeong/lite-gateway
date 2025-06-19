package dev.jazzybyte.lite.gateway.route

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import java.net.URI

class InMemoryRouteLocatorTest {

    @Test
    fun `should match multiple routes`() {
        val route1 = Route(
            id = "r1",
            predicates = listOf(RoutePredicate { it.request.uri.path.startsWith("/api") }),
            uri = URI.create("http://api-1")
        )

        val route2 = Route(
            id = "r2",
            predicates = listOf(RoutePredicate { it.request.uri.path.contains("users") }),
            uri = URI.create("http://api-2")
        )

        val locator = InMemoryRouteLocator(listOf(route1, route2))
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users").build())

        val matchedRoutes = locator.locate(exchange).collectList().block()

        assertNotNull(matchedRoutes)
        assertEquals(2, matchedRoutes!!.size)
        assertEquals(setOf("r1", "r2"), matchedRoutes.map { it.id }.toSet())
    }

    @Test
    fun `should return null if no route matches`() {
        val route = Route(
            id = "no-match",
            predicates = listOf(
                RoutePredicate { ex -> ex.request.uri.path.startsWith("/admin") }
            ),
            uri = URI.create("http://admin")
        )

        val locator = InMemoryRouteLocator(listOf(route))
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/user").build())

        assertTrue(locator.locate(exchange).collectList().block()!!.isEmpty())
    }
}