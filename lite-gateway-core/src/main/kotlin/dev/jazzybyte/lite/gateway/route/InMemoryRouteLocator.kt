package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class InMemoryRouteLocator(
    private val routes: List<Route>
) : RouteLocator {

    override fun locate(exchange: ServerWebExchange): Mono<Route> {
    return routes.filter { route ->
        route.predicates.all { p -> p.matches(exchange) }
    }.firstNotNullOf { Mono.empty() }
    }
}