package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.filter.GatewayContext
import dev.jazzybyte.lite.gateway.filter.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.GatewayFilterChain
import reactor.core.publisher.Mono

/**
 * GatewayFilterChain 인터페이스의 기본 구현체.
 * 필터 목록을 순회하며 각 필터를 실행하고, 마지막에는 최종 액션을 수행한다.
 * 
 * 스레드 안전성을 보장하기 위해 immutable 방식으로 구현되었으며,
 * 각 필터 실행 시 새로운 체인 인스턴스를 생성한다.
 */
class DefaultGatewayFilterChain(
    private val filters: List<GatewayFilter>,
    private val finalAction: (GatewayContext) -> Mono<Void>, // 모든 필터가 실행된 후 수행될 최종 액션 (예: 프록시 요청)
    private val index: Int = 0 // 현재 실행할 필터의 인덱스 (immutable)
) : GatewayFilterChain {

    /**
     * 필터 체인을 시작하거나 다음 필터를 실행한다.
     * 
     * 스레드 안전성을 보장하기 위해 각 필터 실행 시 새로운 체인 인스턴스를 생성한다.
     *
     * @param context 현재 요청/응답 컨텍스트
     * @return 필터 체인의 완료를 나타내는 Mono<Void>
     */
    override fun filter(context: GatewayContext): Mono<Void> {
        return Mono.defer {
            if (this.index < filters.size) {
                val currentFilter = filters[this.index]
                // 다음 필터를 위해 새로운 체인 인스턴스 생성 (스레드 안전성 보장)
                val nextChain = DefaultGatewayFilterChain(filters, finalAction, this.index + 1)
                currentFilter.filter(context, nextChain) // 현재 필터 실행 및 다음 체인 전달
            } else {
                // 모든 필터가 실행되었으므로 최종 액션 수행
                finalAction(context)
            }
        }
    }
}
