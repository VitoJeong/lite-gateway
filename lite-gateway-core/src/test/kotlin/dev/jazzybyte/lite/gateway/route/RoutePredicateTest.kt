package dev.jazzybyte.lite.gateway.route

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class RoutePredicateTest {

    @Test
    fun `matches by exact path`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com/exact"))

        PathPredicate("/exact").matches(
            serverWebExchange
        ).also { assertTrue(it) }
    }


    @Test
    fun `matches by path has wildcard`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com/test"))

        PathPredicate("/*").matches(
            serverWebExchange
        ).also { assertTrue(it) }

    }

    @Test
    fun `does not match different path`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com/other"))

        PathPredicate("/test").matches(
            serverWebExchange
        ).also { assertFalse(it) }
    }

    @Test
    fun `matches path with trailing slash`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com/test/"))

        PathPredicate("/test/").matches(
            serverWebExchange
        ).also { assertTrue(it) }
    }

    @Test
    fun `matches path with query parameters`() {
        val serverWebExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("https://test.com/test?param=value"))

        PathPredicate("/test").matches(
            serverWebExchange
        ).also { assertTrue(it) }
    }

    @Test
    fun `does not match path with regex pattern`() {
        val serverWebExchange =
            MockServerWebExchange.from(
                MockServerHttpRequest.get("https://test.com/api/vv/resource"))

        PathPredicate("/api/v{\\d}/resource").matches(
            serverWebExchange
        ).also { assertTrue(it) }
    }

    @Test
    fun `matches path with regex pattern`() {
        val serverWebExchange =
            MockServerWebExchange.from(
                MockServerHttpRequest.get("https://test.com/api/v22/resource"))

        PathPredicate("/api/v{\\d{1,3}}/{[a-zA-Z]{1,10}}").matches(
            serverWebExchange
        ).also { assertTrue(it) }
    }
}