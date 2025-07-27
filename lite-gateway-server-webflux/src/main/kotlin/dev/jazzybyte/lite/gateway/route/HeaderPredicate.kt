package dev.jazzybyte.lite.gateway.route;

import dev.jazzybyte.lite.gateway.context.RequestContext

class HeaderPredicate (
    text: String
): RoutePredicate {

    private val name: String
    private val value: String?

    init {
        val args = text.split(",").map { it.trim() }
        this.name = args.getOrElse(0) { throw IllegalArgumentException("Header name must not be empty.") }
        this.value = args.getOrElse(1) { null }
    }

    override fun matches(context: RequestContext): Boolean {

        val headerValues = context.headers(name)

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