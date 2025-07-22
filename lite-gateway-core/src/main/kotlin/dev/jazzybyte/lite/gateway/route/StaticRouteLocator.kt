package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 메모리 내에서 정적 RouteLocator 구현체
 * 주어진 라우트 목록을 기반으로 요청에 맞는 라우트를 찾아 반환합니다.
 *
 * 라우팅 설정들은 시스템이 실행될때 빈으로 등록되고 변경되지 않아, Flux가 아닌 List<Route>를 사용한다.
 */
class StaticRouteLocator(
    val routes: List<Route>,
) : RouteLocator {

    // 라우트는 order에 따라 정렬하고 order가 같으면 들어온 순서를 유지하도록 한다.
    private val sortedRoutes: List<Route> = routes.sortedWith(compareBy<Route> { it.order }
        .thenBy { routes.indexOf(it) })

    override fun locate(exchange: ServerWebExchange): Mono<Route> {
        return Flux.fromIterable(sortedRoutes)
            .filter { it.predicates.all { p -> p.matches(exchange) } }
            .next()
    }
}