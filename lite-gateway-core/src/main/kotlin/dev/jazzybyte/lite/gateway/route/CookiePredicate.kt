package dev.jazzybyte.lite.gateway.route;

import org.springframework.web.server.ServerWebExchange

class CookiePredicate (
    private val name: String,
    private val pattern: String? = null
): RoutePredicate {

    override fun matches(exchange: ServerWebExchange): Boolean {

        val cookies = exchange.request.cookies[name] ?: return false

        // value 가 null인 경우, 해당 Cookie가 존재하는지만 확인
        pattern ?: return cookies.isNotEmpty()

        // 정규표현식으로 하나라도 매칭되면 통과
        cookies.forEach { cookie ->
            if (Regex(pattern).matches(cookie.value)) {
                return true
            }
        }

        return false
    }
}