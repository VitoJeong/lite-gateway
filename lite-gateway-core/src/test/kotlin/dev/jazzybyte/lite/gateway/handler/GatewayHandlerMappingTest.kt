package dev.jazzybyte.lite.gateway.handler

import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteLocator
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class GatewayHandlerMappingTest {


    var locator: RouteLocator = mockk<RouteLocator>()
    var handler: FilterHandler = mockk<FilterHandler>()

    // GatewayHandlerMapping 클래스의 익명 객체를 생성하여 테스트 구현
    // -> getHandlerInternal 메서드를 테스트하기 위한 목적으로 사용
    private val gatewayHandlerMapping = GatewayHandlerMapping(locator, handler)

    @Test
    fun `get handler internal`() {

        // given
        val route = Route(
            id = "test-route",
            uri = "https://test.com",
            predicate = { it.request.uri.path.startsWith("/v1") }
        )

        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/v1/test").build()
        )

        every { locator.locate(exchange) } returns Mono.just(route)

        // when
        val handler = gatewayHandlerMapping.resolveHandler(exchange).block()

        // then
        assertNotNull(handler, "Handler should not be null")
        assertThat(exchange.attributes["matchedRoute"]).isNotNull
        assertThat(exchange.attributes["matchedRoute"]).isEqualTo(route)
    }

}