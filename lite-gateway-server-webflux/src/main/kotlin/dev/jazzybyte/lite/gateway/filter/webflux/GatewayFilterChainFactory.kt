package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.filter.GatewayContext
import dev.jazzybyte.lite.gateway.filter.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.GatewayFilterChain
import reactor.core.publisher.Mono

/**
 * 게이트웨이 필터 체인을 생성하는 팩토리 클래스
 * 
 * 오류 처리 기능이 포함된 필터 체인과 기본 필터 체인 중 선택하여 생성할 수 있다.
 */
object GatewayFilterChainFactory {

    /**
     * 오류 처리 기능이 포함된 필터 체인을 생성한다.
     * 
     * @param filters 실행할 필터 목록
     * @param finalAction 모든 필터 실행 후 수행할 최종 액션
     * @param routeId 라우트 식별자 (선택 사항)
     * @param requestId 요청 식별자 (선택 사항)
     * @return 오류 처리 기능이 포함된 필터 체인
     */
    fun createErrorHandlingChain(
        filters: List<GatewayFilter>,
        finalAction: (GatewayContext) -> Mono<Void>,
        routeId: String? = null,
        requestId: String? = null
    ): GatewayFilterChain {
        return ErrorHandlingGatewayFilterChain(
            filters = filters,
            finalAction = finalAction,
            routeId = routeId,
            requestId = requestId
        )
    }

    /**
     * 기본 필터 체인을 생성한다.
     * 
     * @param filters 실행할 필터 목록
     * @param finalAction 모든 필터 실행 후 수행할 최종 액션
     * @return 기본 필터 체인
     */
    fun createDefaultChain(
        filters: List<GatewayFilter>,
        finalAction: (GatewayContext) -> Mono<Void>
    ): GatewayFilterChain {
        return DefaultGatewayFilterChain(
            filters = filters,
            finalAction = finalAction
        )
    }

    /**
     * 설정에 따라 적절한 필터 체인을 생성한다.
     * 
     * @param filters 실행할 필터 목록
     * @param finalAction 모든 필터 실행 후 수행할 최종 액션
     * @param enableErrorHandling 오류 처리 기능 활성화 여부 (기본값: true)
     * @param routeId 라우트 식별자 (선택 사항)
     * @param requestId 요청 식별자 (선택 사항)
     * @return 설정에 맞는 필터 체인
     */
    fun createChain(
        filters: List<GatewayFilter>,
        finalAction: (GatewayContext) -> Mono<Void>,
        enableErrorHandling: Boolean = true,
        routeId: String? = null,
        requestId: String? = null
    ): GatewayFilterChain {
        return if (enableErrorHandling) {
            createErrorHandlingChain(filters, finalAction, routeId, requestId)
        } else {
            createDefaultChain(filters, finalAction)
        }
    }
}