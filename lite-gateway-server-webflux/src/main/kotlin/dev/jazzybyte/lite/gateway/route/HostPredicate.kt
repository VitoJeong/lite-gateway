package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.RequestContext
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import org.springframework.util.AntPathMatcher

class HostPredicate (
    private val pattern: String
) : RoutePredicate {

    private val matcher: AntPathMatcher = AntPathMatcher(".")

    override fun matches(context: RequestContext): Boolean {
        // 요청의 호스트가 지정된 호스트와 일치하는지 확인
        return context.host().let { host ->
            matcher.match(pattern, host)
        }
    }
}