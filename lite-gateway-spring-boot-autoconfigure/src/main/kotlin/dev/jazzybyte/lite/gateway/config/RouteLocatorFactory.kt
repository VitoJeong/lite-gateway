package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import dev.jazzybyte.lite.gateway.route.RouteLocator
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import dev.jazzybyte.lite.gateway.route.StaticRouteLocator
import dev.jazzybyte.lite.gateway.util.ReflectionUtil
import dev.jazzybyte.lite.gateway.exception.PredicateInstantiationException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import java.lang.reflect.Constructor

private val log = KotlinLogging.logger {}

class RouteLocatorFactory {

    companion object {

        // Predicate 클래스들을 동적으로 로드하기 위한 맵(Predicate Prefix -> 클래스)
        private val predicateClasses: Map<String, Class<out RoutePredicate>> =
            ReflectionUtil.findClassesOfType("dev.jazzybyte.lite.gateway.route", RoutePredicate::class.java)
                .associateBy { it.simpleName.removeSuffix("Predicate") }
                .also {
                    log.info { "Initializing predicate classes: $it" }
                }

        fun create(routeDefinitions: @NotNull @Valid MutableList<RouteDefinition>): RouteLocator {
            val routes = routeDefinitions
                .map { def ->
                    Route(
                        id = def.id,
                        uri = def.uri,
                        predicates = initPredicates(def),
                        order = if (def.order < 0) {
                            log.warn { "Route '${def.id}' has negative order ${def.order}. Setting to 0." }
                            0
                        } else {
                            def.order
                        }
                    )
                }
                .sortedBy { it.order }

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
                    handlePredicateInstantiationError(
                        e,
                        def.id,
                        predicateDef.name,
                        predicateClass,
                        predicateDef.parsedArgs
                    )
                }
            }

        private fun handlePredicateInstantiationError(
            cause: Exception,
            routeId: String,
            predicateName: String,
            predicateClass: Class<out RoutePredicate>,
            args: Array<String>,
        ): Nothing {
            val constructors = predicateClass.constructors
            val availableConstructors = constructors.joinToString(", ") { constructor ->
                val paramTypes = constructor.parameterTypes.joinToString(", ") { it.simpleName }
                "${constructor.name}($paramTypes)"
            }

            val argTypes = args.map { arg ->
                when {
                    arg.toIntOrNull() != null -> "Int"
                    arg.toBooleanStrictOrNull() != null -> "Boolean"
                    arg.toDoubleOrNull() != null -> "Double"
                    else -> "String"
                }
            }.joinToString(", ")

            val detailedMessage = when (cause) {
                is IllegalArgumentException -> {
                    if (cause.message?.contains("No suitable constructor found") == true) {
                        "Constructor matching failed. " +
                                "Available constructors: [$availableConstructors]. " +
                                "Provided argument types: [$argTypes]. " +
                                "Ensure the predicate class '${predicateClass.name}' has a constructor that matches the provided arguments."
                    } else {
                        "Argument type mismatch or invalid arguments. " +
                                "Available constructors: [$availableConstructors]. " +
                                "Provided argument types: [$argTypes]. " +
                                "Original error: ${cause.message}"
                    }
                }

                is InstantiationException -> {
                    "Cannot instantiate predicate class '${predicateClass.name}'. " +
                            "Ensure the class is not abstract and has accessible constructors. " +
                            "Available constructors: [$availableConstructors]"
                }

                is IllegalAccessException -> {
                    "Cannot access constructor of predicate class '${predicateClass.name}'. " +
                            "Ensure the constructor is public. " +
                            "Available constructors: [$availableConstructors]"
                }

                else -> {
                    "Unexpected error during predicate instantiation. " +
                            "Available constructors: [$availableConstructors]. " +
                            "Provided argument types: [$argTypes]. " +
                            "Error: ${cause.message}"
                }
            }

            log.error(cause) {
                "Failed to instantiate predicate '$predicateName' for route '$routeId'. $detailedMessage"
            }

            throw PredicateInstantiationException(
                message = detailedMessage,
                routeId = routeId,
                predicateName = predicateName,
                predicateArgs = args,
                cause = cause
            )
        }
    }

}
