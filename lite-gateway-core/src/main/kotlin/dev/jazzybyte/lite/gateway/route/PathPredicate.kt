package dev.jazzybyte.lite.gateway.route

import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange


class PathPredicate (
    private val pattern: String = ""
): RoutePredicate {

    // 와일드카드 `*`를 지원하는 AntPathMatcher 사용
    private val matcher: AntPathMatcher = AntPathMatcher()

    override fun matches(exchange: ServerWebExchange): Boolean {
        return matcher.match(pattern, exchange.request.path.value())
    }
}