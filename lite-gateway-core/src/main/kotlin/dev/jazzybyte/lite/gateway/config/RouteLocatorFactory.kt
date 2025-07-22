package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import dev.jazzybyte.lite.gateway.route.RouteLocator
import dev.jazzybyte.lite.gateway.route.RoutePredicate
import dev.jazzybyte.lite.gateway.route.StaticRouteLocator
import dev.jazzybyte.lite.gateway.util.ReflectionUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

private val log = KotlinLogging.logger {}

class RouteLocatorFactory {

    companion object {

        // Predicate 클래스들을 동적으로 로드하기 위한 맵(Predicate Prefix -> 클래스)
        val predicateClasses: Map<String, Class<out RoutePredicate>> =
            ReflectionUtil.getClassesFromPackage("dev.jazzybyte.lite.gateway.route")
                .filter { it.simpleName.endsWith("Predicate") && RoutePredicate::class.java.isAssignableFrom(it) }
                .associateBy { it.simpleName.removeSuffix("Predicate") }
                .mapValues { (_, clazz) -> clazz as Class<out RoutePredicate> }
                .also {
                    log.info { "Initializing predicate classes: $it" }
                }

        fun create(routeDefinitions: @NotNull @Valid MutableList<RouteDefinition>): RouteLocator {
            val routes = routeDefinitions
                .sortedBy { it.order }
                .map { def ->
                    Route(
                        id = def.id,
                        uri = def.uri,
                        predicates = initPredicates(def)
                    )
                }

            return StaticRouteLocator(routes).also {
                log.info { "Created RouteLocator with routes: $routes" }
            }
        }

        private fun initPredicates(def: RouteDefinition): List<RoutePredicate> = def.predicates
            .map { predicateDef ->
                val predicateClass = predicateClasses[predicateDef.name]
                    ?: throw IllegalArgumentException("Unknown predicate '${predicateDef.name}' in route definition with ID '${def.id}'")

                try {
                    // 배열이 하나일 경우 String으로 변환
                    if (predicateDef.parsedArgs.size == 1) {
                        ReflectionUtil.createInstanceOfType(predicateClass, predicateDef.parsedArgs[0])
                    } else {
                        ReflectionUtil.createInstanceOfType(predicateClass, *predicateDef.parsedArgs)
                    }
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Failed to create an instance of predicate '${predicateDef.name}' in route definition with ID '${def.id}'. " +
                                "Ensure the predicate class '${predicateClass.name}' and its arguments are valid.", e
                    )
                }
            }
    }

}
