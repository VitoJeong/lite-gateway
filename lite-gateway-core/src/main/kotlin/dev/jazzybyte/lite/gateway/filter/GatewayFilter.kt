package dev.jazzybyte.lite.gateway.filter

import reactor.core.publisher.Mono

/**
 * 프레임워크 독립적인 게이트웨이 필터 인터페이스.
 * 요청 처리 전/후 로직을 정의한다.
 */
interface GatewayFilter {
    /**
     * 필터 로직을 수행한다.
     *
     * @param context 현재 요청/응답 컨텍스트
     * @param chain 다음 필터 또는 최종 핸들러로 제어를 넘기기 위한 체인
     * @return 필터 체인의 완료를 나타내는 Mono<Void>
     */
    fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void>
}