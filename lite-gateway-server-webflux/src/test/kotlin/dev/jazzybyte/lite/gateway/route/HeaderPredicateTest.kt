package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.ServerWebExchangeRequestContext
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class HeaderPredicateTest {

    @Test
    fun `matches when header exists`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com")
                .header("X-Test-Header", "value1")
        )

        HeaderPredicate("X-Test-Header").matches(
            ServerWebExchangeRequestContext(serverWebExchange)
        ).also { assertTrue(it) }
    }

    @Test
    fun `does not match when header value does not match pattern`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.put("https://test.com")
                .header("X-Test-Header", "value1")
        )

        HeaderPredicate("X-Test-Header,value2").matches(
            ServerWebExchangeRequestContext(serverWebExchange)
        ).also { assertFalse(it) }
    }

    @Test
    fun `does not match when header does not exist`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com")
        )

        HeaderPredicate("X-Test-Header").matches(
            ServerWebExchangeRequestContext(serverWebExchange)
        ).also { assertFalse(it) }
    }

    @Test
    fun `matches when header exists without value`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com")
                .header("X-Test-Header", "value1")
        )

        HeaderPredicate("X-Test-Header").matches(
            ServerWebExchangeRequestContext(serverWebExchange)
        ).also { assertTrue(it) }
    }

    @Test
    fun `matches when header value matches pattern`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("https://test.com")
                .header("X-Test-Header", "value123")
        )

        HeaderPredicate("X-Test-Header, value\\d+").matches(
            ServerWebExchangeRequestContext(serverWebExchange)
        ).also { assertTrue(it) }
    }
}