package dev.jazzybyte.lite.gateway.route

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.server.ServerWebExchange

class RouteTest {

    @Test
    fun `default HTTP port`() {
        val route = Route(
            id = "1",
            uri = "http://test.com",
            predicate = { true }
        )

        assertThat(route.uri)
            .hasHost("test.com")
            .hasScheme("http")
            .hasPort(80)
    }

    @Test
    fun `default HTTPS port`() {
        val route = Route(
            id = "1",
            uri = "https://test.com",
            predicate = { true }
        )

        assertThat(route.uri)
            .hasHost("test.com")
            .hasScheme("https")
            .hasPort(443)
    }

    @Test
    fun `full Uri`() {
        val route: Route = Route(
            id = "1",
            uri = "http://test.com:8080",
            predicate = { true }
        )

        assertThat(route.uri)
            .hasHost("test.com")
            .hasScheme("http")
            .hasPort(8080)
    }

    @Test
    fun `null Scheme`() {
        Assertions.assertThatThrownBy {
            Route(
                id = "1",
                uri = "/pathOnly",
                predicate = { true }
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
    }


}