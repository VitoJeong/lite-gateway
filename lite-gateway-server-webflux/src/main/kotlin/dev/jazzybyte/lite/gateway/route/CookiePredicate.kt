package dev.jazzybyte.lite.gateway.route;

import dev.jazzybyte.lite.gateway.context.RequestContext
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate


class CookiePredicate (
    private val key: String,
    private val pattern: String = ".*"
): RoutePredicate {

    override fun matches(context: RequestContext): Boolean {

        val cookies = context.cookies(key) ?: return false

        // 정규표현식으로 하나라도 매칭되면 통과
        cookies.forEach { cookie ->
            if (Regex(pattern).matches(cookie.value)) {
                return true
            }
        }

        return false
    }
}