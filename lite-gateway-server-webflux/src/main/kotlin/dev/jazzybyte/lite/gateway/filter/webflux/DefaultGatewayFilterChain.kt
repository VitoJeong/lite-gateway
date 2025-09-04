package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.filter.core.GatewayContext
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilterChain
import reactor.core.publisher.Mono

/**
 * GatewayFilterChain 인터페이스의 기본 구현체.
 * 필터 목록을 순회하며 각 필터를 실행하고, 마지막에는 최종 액션을 수행합니다.
 */
class DefaultGatewayFilterChain(
    private val filters: List<GatewayFilter>,
    private val finalAction: (GatewayContext) -> Mono<Void> // 모든 필터가 실행된 후 수행될 최종 액션 (예: 프록시 요청)
) : GatewayFilterChain {

    private var index: Int = 0

    /**
     * 필터 체인을 시작하거나 다음 필터를 실행합니다.
     *
     * @param context 현재 요청/응답 컨텍스트
     * @return 필터 체인의 완료를 나타내는 Mono<Void>
     */
    override fun filter(context: GatewayContext): Mono<Void> {
        return Mono.defer {
            if (this.index < filters.size) {
                val currentFilter = filters[this.index]
                this.index++ // 다음 필터를 위해 인덱스 증가
                currentFilter.filter(context, this) // 현재 필터 실행 및 자신(체인) 전달
            } else {
                // 모든 필터가 실행되었으므로 최종 액션 수행
                finalAction(context)
            }
        }
    }
}
