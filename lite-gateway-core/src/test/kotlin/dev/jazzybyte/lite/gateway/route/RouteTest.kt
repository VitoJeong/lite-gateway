package dev.jazzybyte.lite.gateway.route

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.server.ServerWebExchange

class RouteTest {

 @Test
 fun `default HTTP port`() {
  val route = Route.builder()
   .id("1")
   .predicate(object : RoutePredicate {
    override fun matches(exchange: ServerWebExchange): Boolean {
     return true
    }
   })
   .predicate { true }
   .uri("http://test.com")
   .build()

  assertThat(route.uri)
   .hasHost("test.com")
   .hasScheme("http")
   .hasPort(80)
 }

 @Test
 fun `default HTTPS port`() {
  val route = Route.builder()
   .id("1")
   .predicate { true }
   .uri("https://test.com")
   .build()

  assertThat(route.uri)
   .hasHost("test.com")
   .hasScheme("https")
   .hasPort(443)
 }


 @Test
 fun `full Uri`() {
  val route: Route = Route.builder()
   .id("1")
   .predicate { true }
   .uri("http://test.com:8080")
   .build()

  assertThat(route.uri)
   .hasHost("test.com")
   .hasScheme("http")
   .hasPort(8080)
 }

 @Test
 fun `null Scheme`() {
  Assertions.assertThatThrownBy {
   Route.builder()
    .id("1")
    .predicate { exchange -> true }
    .uri("/pathonly")
  }
   .isInstanceOf(IllegalArgumentException::class.java)
 }


}