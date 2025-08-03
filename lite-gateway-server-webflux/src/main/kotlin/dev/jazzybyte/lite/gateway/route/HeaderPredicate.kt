package dev.jazzybyte.lite.gateway.route;

import dev.jazzybyte.lite.gateway.context.RequestContext
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate

class HeaderPredicate (
    private val name: String,
    private val value: String = ".*"
): RoutePredicate {

    override fun matches(context: RequestContext): Boolean {

        val headerValues = context.headers(name)

        // 정규표현식으로 하나라도 매칭되면 통과
        headerValues.forEach { headerValue ->
            if (Regex(value).matches(headerValue)) {
                return true
            }
        }

        return false
    }
}