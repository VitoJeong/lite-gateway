package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.config.validation.UriValidator
import dev.jazzybyte.lite.gateway.exception.PredicateDiscoveryException
import dev.jazzybyte.lite.gateway.exception.PredicateInstantiationException
import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import dev.jazzybyte.lite.gateway.util.ReflectionUtil
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * 라우트 생성 로직을 담당하는 클래스입니다.
 * RouteDefinition을 Route 객체로 변환하고 관련 검증을 수행합니다.
 */
class RouteBuilder(
    private val predicateRegistry: PredicateRegistry,
    private val uriValidator: UriValidator
) {

    /**
     * RouteDefinition을 Route 객체로 변환합니다.
     */
    fun buildRoute(def: RouteDefinition): Route {
        // 라우트 정의 검증
        validateRouteDefinition(def)

        // Predicate 인스턴스 생성
        val predicates = createPredicates(def)

        // Route 객체 생성
        val order = validateAndNormalizeOrder(def.id, def.order)
        return Route(
            id = def.id,
            uri = def.uri.toString(),
            order = order,
            predicates = predicates
        )
    }

    /**
     * 라우트 정의의 유효성을 검증합니다.
     */
    private fun validateRouteDefinition(def: RouteDefinition) {
        // 필수 필드 검증
        if (def.id.isBlank()) {
            throw RouteConfigurationException(
                message = "Route ID cannot be empty or blank",
                routeId = def.id
            )
        }

        if (def.uri.toString().isBlank()) {
            throw RouteConfigurationException(
                message = "Route URI cannot be empty or blank",
                routeId = def.id
            )
        }

        // Filter 정의 검증
        def.filters.forEachIndexed { index, filterDef ->
            if (filterDef.name.isBlank()) {
                throw RouteConfigurationException(
                    message = "Filter name cannot be empty or blank at index $index",
                    routeId = def.id
                )
            }
        }

        // URI 형식 검증
        uriValidator.validateUriFormat(def.uri.toString(), def.id)

        log.debug { "Route definition validation passed for route '${def.id}'" }
    }

    /**
     * RouteDefinition으로부터 Predicate 인스턴스들을 생성합니다.
     */
    private fun createPredicates(def: RouteDefinition): List<RoutePredicate> {
        return def.predicates.map { predicateDef ->
            // 빈 Predicate 이름 검증
            if (predicateDef.name.isBlank()) {
                throw PredicateDiscoveryException(
                    message = "Unknown predicate '${predicateDef.name}' in route definition with ID '${def.id}'. " +
                            "Available predicates: ${predicateRegistry.getAvailablePredicateNames()}",
                    predicateName = predicateDef.name
                )
            }
            
            val predicateClass = predicateRegistry.getPredicateClass(predicateDef.name)
                ?: throw PredicateDiscoveryException(
                    message = "Unknown predicate '${predicateDef.name}' in route definition with ID '${def.id}'. " +
                            "Available predicates: ${predicateRegistry.getAvailablePredicateNames()}",
                    predicateName = predicateDef.name
                )

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
    }

    /**
     * Predicate 인스턴스화 오류를 처리합니다.
     */
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
                            "Check if the arguments can be converted to the expected parameter types."
                }
            }
            is InstantiationException -> {
                "Failed to instantiate predicate class '${predicateClass.name}'. " +
                        "The class may be abstract or an interface. " +
                        "Available constructors: [$availableConstructors]."
            }
            is IllegalAccessException -> {
                "Access denied when trying to instantiate predicate class '${predicateClass.name}'. " +
                        "The constructor may not be public. " +
                        "Available constructors: [$availableConstructors]."
            }
            else -> {
                "Unexpected error during predicate instantiation. " +
                        "Available constructors: [$availableConstructors]. " +
                        "Provided argument types: [$argTypes]. " +
                        "Error details: ${cause.message}"
            }
        }

        throw PredicateInstantiationException(
            message = detailedMessage,
            routeId = routeId,
            predicateName = predicateName,
            predicateArgs = args,
            cause = cause
        )
    }

    /**
     * order 값을 검증하고 정규화합니다.
     */
    private fun validateAndNormalizeOrder(routeId: String, order: Int): Int {
        return if (order < 0) {
            log.warn { "Route '$routeId' has negative order $order. Setting to 0." }
            0
        } else {
            order
        }
    }
}