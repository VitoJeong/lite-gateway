package dev.jazzybyte.lite.gateway.route

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class RoutePredicateTest {

 @Test
 fun `matches by path`() {
  val serverWebExchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://test.com/test"))


  val pathPredicate = PathPredicate("/test")
  pathPredicate.matches(
   serverWebExchange
  ).also { assertTrue(it) }

 }

 @Test
 fun `matches by path has regex`() {
  val serverWebExchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://test.com/test"))

  PathPredicate("/*").matches(
   serverWebExchange
  ).also { assertTrue(it) }

 }
}