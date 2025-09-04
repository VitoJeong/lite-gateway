package dev.jazzybyte.lite.gateway.filter.core

import reactor.core.publisher.Mono

/**
 * 게이트웨이 필터 체인 인터페이스.
 * 다음 필터 또는 최종 핸들러로 요청을 전달하는 역할을 합니다.
 */
interface GatewayFilterChain {
    /**
     * 현재 컨텍스트를 다음 필터 또는 최종 핸들러로 전달합니다.
     *
     * @param context 현재 요청/응답 컨텍스트
     * @return 필터 체인의 완료를 나타내는 Mono<Void>
     */
    fun filter(context: GatewayContext): Mono<Void>
}
