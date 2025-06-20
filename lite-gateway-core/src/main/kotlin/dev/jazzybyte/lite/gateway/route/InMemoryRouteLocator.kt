package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class InMemoryRouteLocator(
    private val routes: List<Route>,
) : RouteLocator {

    // 라우트는 order에 따라 정렬하고 order가 같으면 들어온 순서를 유지하도록 한다.
    private val sortedRoutes: List<Route> = routes.sortedWith(compareBy<Route> { it.order }.thenBy { routes.indexOf(it) })

    override fun locate(exchange: ServerWebExchange): Mono<Route> {
        return Flux.fromIterable(sortedRoutes)
            .filter { it.predicates.all { p -> p.matches(exchange) } }
            .next()
    }
}