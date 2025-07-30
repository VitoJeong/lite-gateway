package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.RequestContext
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import org.springframework.util.AntPathMatcher


class PathPredicate (
    private val pattern: String = ""
): RoutePredicate {

    // 와일드카드 `*`를 지원하는 AntPathMatcher 사용
    private val matcher = AntPathMatcher()

    override fun matches(context: RequestContext): Boolean {
        return matcher.match(pattern, context.path())
    }
}