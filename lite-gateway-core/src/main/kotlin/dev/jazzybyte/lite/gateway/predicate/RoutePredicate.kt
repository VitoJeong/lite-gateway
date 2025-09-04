package dev.jazzybyte.lite.gateway.predicate

import dev.jazzybyte.lite.gateway.context.RequestContext

/**
 * 요청이 조건에 부합한지 판단하는 인터페이스
 * 구현 클래스는 제공된 RequestContext를 기반으로 매칭 여부를 판단하는 로직을 정의해야 한다.
 */
fun interface RoutePredicate {
    fun matches(context: RequestContext): Boolean
}