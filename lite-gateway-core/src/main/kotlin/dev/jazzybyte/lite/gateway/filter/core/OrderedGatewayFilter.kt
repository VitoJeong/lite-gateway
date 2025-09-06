package dev.jazzybyte.lite.gateway.filter.core

import org.springframework.core.Ordered

/**
 * 실행 순서를 지원하는 게이트웨이 필터 인터페이스.
 * Spring의 Ordered 인터페이스를 확장하여 표준 순서 지원을 제공한다.
 * 
 * 필터는 order 값에 따라 오름차순으로 정렬되어 실행된다.
 * 낮은 order 값을 가진 필터가 먼저 실행된다.
 */
interface OrderedGatewayFilter : GatewayFilter, Ordered {
    
    /**
     * 필터의 실행 순서를 반환한다.
     * 
     * @return 필터의 실행 순서 (낮은 값일수록 먼저 실행됨)
     */
    override fun getOrder(): Int
}