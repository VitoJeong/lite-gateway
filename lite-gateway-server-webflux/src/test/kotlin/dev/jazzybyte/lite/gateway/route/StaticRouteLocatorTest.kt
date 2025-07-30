package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.ServerWebExchangeRequestContext
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class StaticRouteLocatorTest {

    @Test
    fun `should match multiple routes`() {
        val route1 = Route(
            id = "r1",
            predicate = { it.path().startsWith("/api") },
            uri = "http://api-1.dev"
        )

        val route2 = Route(
            id = "r2",
            predicate = { it.path().contains("users") },
            uri = "http://api-1.dev"
        )
        val locator = StaticRouteLocator(listOf(route1, route2))
        val exchange = MockServerWebExchange.from(MockServerHttpRequest
            .get("http://api-1.dev/api/users")
            .build())

        val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

        assertNotNull(matched)
        assertThat(matched!!.id).isEqualTo("r1")
    }

    @Test
    fun `should return null if no route matches`() {
        val route = Route(
            id = "no-match",
            predicate = { it -> it.path().startsWith("/admin") },
            uri = "http://admin"
        )

        val locator = StaticRouteLocator(listOf(route))
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/user").build())

        assertTrue(locator.locate(ServerWebExchangeRequestContext(exchange)).blockOptional().isEmpty)
    }
}