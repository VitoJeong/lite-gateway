package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux

/**
 * 요청에 대한 라우팅 정보를 제공하는 인터페이스
 * 구현 클래스는 ServerWebExchange를 기반으로 라우트 목록을 반환해야 합니다.
 */
interface RouteLocator {

    /**
     * 요청에 맞는 라우팅 정보들을 찾아 반환한다.
     * Flux 로 반환하는 이유 => reactive stream 기반의 동적 라우트 탐색이 가능하도록 하기 위함
     */
    fun locate(exchange: ServerWebExchange): Flux<Route>
}