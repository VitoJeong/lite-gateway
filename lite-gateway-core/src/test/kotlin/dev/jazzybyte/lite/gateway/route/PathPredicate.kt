package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.context.RequestContext
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate

class PathPredicate : RoutePredicate {
    override fun matches(context: RequestContext): Boolean {
        // For testing purposes, always return true
        return true
    }
}
