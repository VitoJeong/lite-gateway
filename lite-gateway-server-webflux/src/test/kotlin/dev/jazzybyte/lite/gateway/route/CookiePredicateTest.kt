package dev.jazzybyte.lite.gateway.route

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpCookie
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class CookiePredicateTest {

    @Test
    fun `matches when cookie value matches pattern`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com")
                .cookie(HttpCookie("X-Test-Cookie", "value1"))
        )

        CookiePredicate(key = "X-Test-Cookie", pattern = "value\\d+").matches(
            serverWebExchange
        ).also { assertTrue(it) }
    }

    @Test
    fun `matches when cookie exists but pattern is null`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com")
                .cookie(HttpCookie("X-Test-Cookie", "value1"))
        )

        CookiePredicate(key = "X-Test-Cookie", pattern = null).matches(
            serverWebExchange
        ).also { assertTrue(it) }
    }

    @Test
    fun `does not match when cookie value does not match pattern`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com")
                .cookie(HttpCookie("X-Test-Cookie", "invalidValue"))
        )

        CookiePredicate(key = "X-Test-Cookie", pattern = "value\\d+").matches(
            serverWebExchange
        ).also { assertFalse(it) }
    }
}