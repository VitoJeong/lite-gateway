package dev.jazzybyte.lite.gateway.route

import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange


class PathPredicate (
    private val path: String = ""
): RoutePredicate {

    // 와일드카드 `*`를 지원하는 AntPathMatcher 사용
    val matcher: AntPathMatcher = AntPathMatcher()

    override fun matches(exchange: ServerWebExchange): Boolean {
        return matcher.match(path, exchange.request.path.value())
    }
}