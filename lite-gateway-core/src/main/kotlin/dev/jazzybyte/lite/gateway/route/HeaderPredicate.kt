package dev.jazzybyte.lite.gateway.route;

import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange

class HeaderPredicate (
    text: String
): RoutePredicate {

    private val name: String
    private val value: String?

    init {
        val args = StringUtils.tokenizeToStringArray(text, ",")
        this.name = args.getOrElse(0) { throw IllegalArgumentException("Header name must not be empty.") }
        this.value = args.getOrElse(1) { null }
    }

    override fun matches(exchange: ServerWebExchange): Boolean {

        val headerValues = exchange.request.headers.get(name) ?: return false

        // value 가 null인 경우, 해당 헤더가 존재하는지만 확인
        value ?: return headerValues.isNotEmpty()

        // 정규표현식으로 하나라도 매칭되면 통과
        headerValues.forEach { headerValue ->
            if (Regex(value).matches(headerValue)) {
                return true
            }
        }

        return false
    }
}