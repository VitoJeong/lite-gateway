package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.ServerWebExchangeRequestContext
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class HostPredicateTest {

    @Test
    fun `matches when host matches pattern`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://test.com")
        )

        HostPredicate("test.com").matches(
            ServerWebExchangeRequestContext(serverWebExchange)
        ).also { assertTrue(it) }
    }

    @Test
    fun `does not match when host does not match pattern`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://example.com")
        )

        HostPredicate("test.com").matches(
            ServerWebExchangeRequestContext(serverWebExchange)
        ).also { assertFalse(it) }
    }

    @Test
    fun `matches when host matches wildcard pattern`() {
        val serverWebExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("https://sub.test.com")
        )

        HostPredicate("*.test.com").matches(
            ServerWebExchangeRequestContext(serverWebExchange)
        ).also { assertTrue(it) }
    }

}