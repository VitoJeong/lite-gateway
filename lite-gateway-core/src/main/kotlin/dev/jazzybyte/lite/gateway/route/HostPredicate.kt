package dev.jazzybyte.lite.gateway.route

import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange

class HostPredicate (
    private val pattern: String
) : RoutePredicate {

    val matcher: AntPathMatcher = AntPathMatcher(".")


    override fun matches(exchange: ServerWebExchange): Boolean {
        // 요청의 호스트가 지정된 호스트와 일치하는지 확인
        return exchange.request.uri.host.let { host ->
            matcher.match(pattern, host)
        }
    }
}