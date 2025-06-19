package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange

/**
 * 요청이 조건에 부합한지 판단하는 인터페이스
 * 구현 클래스는 제공된 ServerWebExchange를 기반으로 매칭 여부를 판단하는 로직을 정의해야 합니다.
 */
fun interface RoutePredicate {
    fun matches(exchange: ServerWebExchange): Boolean
}