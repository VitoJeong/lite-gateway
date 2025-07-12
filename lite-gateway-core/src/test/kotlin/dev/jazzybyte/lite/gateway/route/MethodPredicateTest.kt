package dev.jazzybyte.lite.gateway.route

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class MethodPredicateTest {

 @Test
 fun `matches should return true for matching HTTP method`() {
  val predicate = MethodPredicate("GET")
  val exchange = MockServerWebExchange.from(
   MockServerHttpRequest.method(HttpMethod.GET, "https://example.com")
  )

  assertTrue(predicate.matches(exchange))
 }

 @Test
 fun `matches should return false for matching HTTP method`() {
  val predicate = MethodPredicate("POST")
  val exchange = MockServerWebExchange.from(
   MockServerHttpRequest.method(HttpMethod.GET, "https://example.com")
  )

  assertFalse(predicate.matches(exchange))
 }

}