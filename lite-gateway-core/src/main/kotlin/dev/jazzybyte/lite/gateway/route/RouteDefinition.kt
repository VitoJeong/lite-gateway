package dev.jazzybyte.lite.gateway.route

import org.springframework.validation.annotation.Validated

@Validated
class RouteDefinition (
    val id: String,
    val uri: String,
    val predicates: List<RoutePredicate>,
//    val filters: List<RouteFilter> = emptyList(),
    val order: Int = 0
) {

}