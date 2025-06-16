package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.function.Predicate

/**
 * Represents a route in the gateway.
 *
 * @property id The unique identifier for the route.
 * @property uri The URI to which the route points.
 */
class Route(val id: String, val predicate: Predicate<ServerWebExchange>, val uri: URI) {

    class Builder {
        private lateinit var id: String
        private lateinit var predicate: Predicate<ServerWebExchange>
        private lateinit var uri: URI

        fun id(id: String) = apply { this.id = id }
        fun predicate(predicate: Predicate<ServerWebExchange>) = apply { this.predicate = predicate }
        fun uri(uri: String) = uri(URI.create(uri))

        fun uri(uri: URI): Builder {
            this.uri = uri
            val scheme = this.uri.scheme
            // URI가 null이거나 비어있는 경우 예외 발생
            require(!scheme.isNullOrBlank()) { "The parameter [$uri] format is incorrect, scheme can not be empty" }
            // URI의 scheme이 localhost인 경우 예외 발생
            if (scheme.equals("localhost", ignoreCase = true)) {
                throw IllegalArgumentException("The parameter [$uri] format is incorrect, scheme can not be localhost")
            }
            // 포트 설정이 없는 경우 기본 포트를 설정
            if (this.uri.port < 0 && scheme.startsWith("http", ignoreCase = true)) {
                val port = if (this.uri.scheme.equals("https", ignoreCase = true)) 443 else 80
                this.uri = UriComponentsBuilder.fromUri(this.uri).port(port).build(false).toUri()
            }

            return this
        }

        fun build(): Route {
            return Route(id, predicate, uri)
        }
    }

    companion object {
        fun builder() = Builder()
    }
}