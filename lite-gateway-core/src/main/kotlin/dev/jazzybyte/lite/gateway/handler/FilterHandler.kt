package dev.jazzybyte.lite.gateway.handler

import dev.jazzybyte.lite.gateway.route.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebHandler
import reactor.core.publisher.Mono
import java.net.URI
import java.net.URLEncoder

private val log = KotlinLogging.logger {}

class FilterHandler : WebHandler {

    private val log = KotlinLogging.logger {}
    private val webClient = WebClient.builder().build()

    override fun handle(exchange: ServerWebExchange): Mono<Void> {
        val matchedRoute = exchange.attributes["matchedRoute"] as? Route ?: return handleNoRoute(exchange)

        val targetUri = matchedRoute.uri.resolve(exchange.request.uri.path + buildQueryString(exchange.request))
        log.info { "Forwarding request to: $targetUri" }

        return forwardRequest(exchange, targetUri)
    }

    private fun handleNoRoute(exchange: ServerWebExchange): Mono<Void> {
        log.warn { "No matchedRoute found in exchange attributes" }
        return exchange.response.setComplete()
    }

    private fun forwardRequest(exchange: ServerWebExchange, targetUri: URI): Mono<Void> {
        return webClient.method(exchange.request.method)
            .uri(targetUri)
            .headers { it.addAll(exchange.request.headers) }
            .body(exchange.request.body)
            .exchangeToMono { clientResponse ->
                exchange.response.statusCode = clientResponse.statusCode()
                exchange.response.headers.putAll(clientResponse.headers().asHttpHeaders())
                exchange.response.writeWith(clientResponse.bodyToFlux(DataBuffer::class.java))
            }
    }

    private fun buildQueryString(request: ServerHttpRequest): String {
        val queryParams = request.queryParams
        return if (queryParams.isEmpty()) "" else {
            val sb = StringBuilder("?")
            queryParams.forEach { (key, values) ->
                values.forEach { value ->
                    sb.append("${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}&")
                }
            }
            sb.removeSuffix("&").toString()
        }
    }
}
