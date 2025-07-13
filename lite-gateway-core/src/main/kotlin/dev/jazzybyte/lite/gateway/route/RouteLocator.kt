package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


interface RouteLocator {

    /**
     * 요청에 맞는 라우팅 정보들을 찾아 반환한다.
     * Flux 로 반환하는 이유 => reactive stream 기반의 동적 라우트 탐색이 가능하도록 하기 위함
     */
    fun locate(exchange: ServerWebExchange): Mono<Route>
}