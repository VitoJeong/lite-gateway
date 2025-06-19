package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux

class InMemoryRouteLocator(
    private val routes: List<Route>
) : RouteLocator {

    override fun locate(exchange: ServerWebExchange): Flux<Route> {
        return Flux.fromIterable(
            routes.filter { route ->
                route.predicates.all { p -> p.matches(exchange) }
            }
        )
    }
}