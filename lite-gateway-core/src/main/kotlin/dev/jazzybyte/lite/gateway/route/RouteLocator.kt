package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.RequestContext
import reactor.core.publisher.Mono

/**
 * 요청에 맞는 라우팅 정보를 찾는 인터페이스
 * 요청을 처리할 라우트를 찾기 위한 메서드를 정의합니다.
 */
interface RouteLocator {

    /**
     * 요청에 맞는 라우팅 정보들을 찾아 반환한다.
     * Flux 로 반환하는 이유 => reactive stream 기반의 동적 라우트 탐색이 가능하도록 하기 위함
     */
    fun locate(context: RequestContext): Mono<Route>

}

