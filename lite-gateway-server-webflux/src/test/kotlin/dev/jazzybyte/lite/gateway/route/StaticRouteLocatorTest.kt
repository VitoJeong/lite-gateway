package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.ServerWebExchangeRequestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class StaticRouteLocatorTest {

    @Test
    fun `should match multiple routes`() {
        val route1 = Route(
            id = "r1",
            predicate = { it.path().startsWith("/api") },
            uri = "http://api-1.dev",
            order = 1
        )

        val route2 = Route(
            id = "r2",
            predicate = { it.path().contains("users") },
            uri = "http://api-1.dev",
            order = 2
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

    @Nested
    inner class PathPredicateTests {

        @Test
        fun `should match exact path`() {
            val route = Route(
                id = "exact-path",
                predicate = PathPredicate("/api/users"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("exact-path")
        }

        @Test
        fun `should match wildcard path pattern`() {
            val route = Route(
                id = "wildcard-path",
                predicate = PathPredicate("/api/**"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/123").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("wildcard-path")
        }

        @Test
        fun `should not match different path`() {
            val route = Route(
                id = "specific-path",
                predicate = PathPredicate("/admin/**"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).blockOptional()

            assertTrue(matched.isEmpty)
        }
    }

    @Nested
    inner class MethodPredicateTests {

        @Test
        fun `should match GET method`() {
            val route = Route(
                id = "get-route",
                predicate = MethodPredicate("GET"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("get-route")
        }

        @Test
        fun `should match POST method`() {
            val route = Route(
                id = "post-route",
                predicate = MethodPredicate("POST"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("post-route")
        }

        @Test
        fun `should not match different method`() {
            val route = Route(
                id = "post-only",
                predicate = MethodPredicate("POST"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).blockOptional()

            assertTrue(matched.isEmpty)
        }
    }

    @Nested
    inner class HostPredicateTests {

        @Test
        fun `should match exact host`() {
            val route = Route(
                id = "exact-host",
                predicate = HostPredicate("api.example.com"),
                uri = "http://backend.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://api.example.com/api").build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("exact-host")
        }

        @Test
        fun `should match wildcard host pattern`() {
            val route = Route(
                id = "wildcard-host",
                predicate = HostPredicate("*.example.com"),
                uri = "http://backend.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://api.example.com/api").build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("wildcard-host")
        }

        @Test
        fun `should not match different host`() {
            val route = Route(
                id = "specific-host",
                predicate = HostPredicate("api.example.com"),
                uri = "http://backend.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://other.example.com/api").build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).blockOptional()

            assertTrue(matched.isEmpty)
        }
    }

    @Nested
    inner class HeaderPredicateTests {

        @Test
        fun `should match header with exact value`() {
            val route = Route(
                id = "header-exact",
                predicate = HeaderPredicate("X-Request-Id", "12345"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                    .header("X-Request-Id", "12345")
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("header-exact")
        }

        @Test
        fun `should match header with regex pattern`() {
            val route = Route(
                id = "header-regex",
                predicate = HeaderPredicate("X-Version", "v[0-9]+"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                    .header("X-Version", "v2")
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("header-regex")
        }

        @Test
        fun `should match header with default wildcard pattern`() {
            val route = Route(
                id = "header-wildcard",
                predicate = HeaderPredicate("Authorization"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                    .header("Authorization", "Bearer token123")
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("header-wildcard")
        }

        @Test
        fun `should not match missing header`() {
            val route = Route(
                id = "header-required",
                predicate = HeaderPredicate("X-Required-Header"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).blockOptional()

            assertTrue(matched.isEmpty)
        }
    }

    @Nested
    inner class CookiePredicateTests {

        @Test
        fun `should match cookie with exact value`() {
            val route = Route(
                id = "cookie-exact",
                predicate = CookiePredicate("session", "abc123"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                    .cookie(HttpCookie("session", "abc123"))
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("cookie-exact")
        }

        @Test
        fun `should match cookie with regex pattern`() {
            val route = Route(
                id = "cookie-regex",
                predicate = CookiePredicate("user_id", "[0-9]+"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                    .cookie(HttpCookie("user_id", "12345"))
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("cookie-regex")
        }

        @Test
        fun `should match cookie with default wildcard pattern`() {
            val route = Route(
                id = "cookie-wildcard",
                predicate = CookiePredicate("token"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                    .cookie(HttpCookie("token", "any-value-here"))
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("cookie-wildcard")
        }

        @Test
        fun `should not match missing cookie`() {
            val route = Route(
                id = "cookie-required",
                predicate = CookiePredicate("required_cookie"),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).blockOptional()

            assertTrue(matched.isEmpty)
        }
    }

    @Nested
    @DisplayName("복합 조건 테스트 - AND 조합")
    inner class MultiplePredicateTests {

        @Test
        fun `should match route with multiple predicates - all conditions met`() {
            val route = Route(
                id = "multi-predicate",
                predicates = listOf(
                    PathPredicate("/api/**"),
                    MethodPredicate("GET"),
                    HeaderPredicate("X-API-Key")
                ),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users")
                    .header("X-API-Key", "secret123")
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("multi-predicate")
        }

        @Test
        fun `should not match route with multiple predicates - one condition not met`() {
            val route = Route(
                id = "multi-predicate-strict",
                predicates = listOf(
                    PathPredicate("/api/**"),
                    MethodPredicate("POST"),  // 이 조건이 맞지 않음
                    HeaderPredicate("X-API-Key")
                ),
                uri = "http://example.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users")  // GET 요청
                    .header("X-API-Key", "secret123")
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).blockOptional()

            assertTrue(matched.isEmpty)
        }

        @Test
        fun `should match complex route with path, method, host and cookie`() {
            val route = Route(
                id = "complex-route",
                predicates = listOf(
                    PathPredicate("/admin/**"),
                    MethodPredicate("GET"),
                    HostPredicate("admin.example.com"),
                    CookiePredicate("admin_session")
                ),
                uri = "http://admin-backend.com"
            )

            val locator = StaticRouteLocator(listOf(route))
            val exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://admin.example.com/admin/dashboard")
                    .cookie(HttpCookie("admin_session", "admin123"))
                    .build()
            )

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("complex-route")
        }
    }

    @Nested
    inner class RoutePriorityTests {

        @Test
        fun `should return route with lower order value first`() {
            val route1 = Route(
                id = "high-priority",
                predicate = PathPredicate("/api/**"),
                uri = "http://priority.com",
                order = 1
            )

            val route2 = Route(
                id = "low-priority",
                predicate = PathPredicate("/api/**"),
                uri = "http://normal.com",
                order = 10
            )

            val locator = StaticRouteLocator(listOf(route2, route1)) // 순서를 바꿔서 추가
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("high-priority")
            assertThat(matched.order).isEqualTo(1)
        }

        @Test
        fun `should maintain insertion order for same order value`() {
            val route1 = Route(
                id = "first-inserted",
                predicate = PathPredicate("/api/**"),
                uri = "http://first.com",
                order = 5
            )

            val route2 = Route(
                id = "second-inserted",
                predicate = PathPredicate("/api/**"),
                uri = "http://second.com",
                order = 5
            )

            val locator = StaticRouteLocator(listOf(route1, route2))
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).block()

            assertNotNull(matched)
            assertThat(matched!!.id).isEqualTo("first-inserted")
        }
    }

    @Nested
    inner class EmptyRouteListTests {

        @Test
        fun `should return empty when no routes defined`() {
            val locator = StaticRouteLocator(emptyList())
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/any/path").build())

            val matched = locator.locate(ServerWebExchangeRequestContext(exchange)).blockOptional()

            assertTrue(matched.isEmpty)
        }
    }
}