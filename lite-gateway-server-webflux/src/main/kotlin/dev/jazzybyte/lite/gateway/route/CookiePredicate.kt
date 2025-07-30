package dev.jazzybyte.lite.gateway.route;

import dev.jazzybyte.lite.gateway.context.RequestContext
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate


class CookiePredicate (
    private val key: String,
    private val pattern: String? = null
): RoutePredicate {

    override fun matches(context: RequestContext): Boolean {

        val cookies = context.cookies(key) ?: return false

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