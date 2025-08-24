package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.config.validation.UriValidator
import dev.jazzybyte.lite.gateway.exception.PredicateDiscoveryException
import dev.jazzybyte.lite.gateway.exception.PredicateInstantiationException
import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import dev.jazzybyte.lite.gateway.route.PredicateDefinition
import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import dev.jazzybyte.lite.gateway.util.ReflectionUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * 라우트 생성 로직을 담당하는 클래스입니다.
 * RouteDefinition을 Route 객체로 변환하고 관련 검증을 수행합니다.
 * 
 * 성능 최적화:
 * - Predicate 인스턴스 캐싱으로 동일한 설정의 Predicate 재사용
 * - 메모리 효율적인 데이터 구조 사용
 */
class RouteBuilder(
    private val predicateRegistry: PredicateRegistry,
    private val uriValidator: UriValidator
) {
    
    // Predicate instance caching for performance optimization
    // Key: "PredicateName:args" (e.g., "Path:/api/**")
    private val predicateInstanceCache = ConcurrentHashMap<String, RoutePredicate>()
    
    companion object {
        // Cache statistics for monitoring
        private var cacheHits = 0L
        private var cacheMisses = 0L
        
        /**
         * Get cache performance statistics
         */
        fun getCacheStatistics(): Map<String, Any> {
            val totalRequests = cacheHits + cacheMisses
            val hitRate = if (totalRequests > 0) (cacheHits.toDouble() / totalRequests * 100) else 0.0
            
            return mapOf(
                "cacheHits" to cacheHits,
                "cacheMisses" to cacheMisses,
                "totalRequests" to totalRequests,
                "hitRate" to "%.2f%%".format(hitRate)
            )
        }
        
        /**
         * Reset cache statistics - useful for testing
         */
        fun resetCacheStatistics() {
            cacheHits = 0L
            cacheMisses = 0L
        }
    }
    
    /**
     * Clear predicate instance cache - useful for testing or memory management
     */
    fun clearPredicateCache() {
        predicateInstanceCache.clear()
        log.debug { "Predicate instance cache cleared" }
    }
    
    /**
     * Get current cache size
     */
    fun getCacheSize(): Int = predicateInstanceCache.size

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
     * 성능 최적화를 위해 동일한 설정의 Predicate는 캐시에서 재사용합니다.
     */
    private fun createPredicates(def: RouteDefinition): List<RoutePredicate> {
        return def.predicates.map { predicateDef ->
            createSinglePredicate(def.id, predicateDef)
        }
    }
    
    /**
     * 단일 Predicate 인스턴스를 생성하거나 캐시에서 가져옵니다.
     */
    private fun createSinglePredicate(routeId: String, predicateDef: PredicateDefinition): RoutePredicate {
        // 빈 Predicate 이름 검증
        if (predicateDef.name.isBlank()) {
            throw PredicateDiscoveryException(
                message = "Unknown predicate '${predicateDef.name}' in route definition with ID '$routeId'. " +
                        "Available predicates: ${predicateRegistry.getAvailablePredicateNames()}",
                predicateName = predicateDef.name
            )
        }
        
        val predicateClass = predicateRegistry.getPredicateClass(predicateDef.name)
            ?: throw PredicateDiscoveryException(
                message = "Unknown predicate '${predicateDef.name}' in route definition with ID '$routeId'. " +
                        "Available predicates: ${predicateRegistry.getAvailablePredicateNames()}",
                predicateName = predicateDef.name
            )

        // Create cache key based on predicate name and arguments
        val cacheKey = createPredicateCacheKey(predicateDef)
        
        // Try to get from cache first
        val cachedPredicate = predicateInstanceCache[cacheKey]
        if (cachedPredicate != null) {
            cacheHits++
            log.debug { "Cache hit for predicate: $cacheKey" }
            return cachedPredicate
        }
        
        // Cache miss - create new instance
        cacheMisses++
        log.debug { "Cache miss for predicate: $cacheKey" }
        
        val predicate = try {
            // 배열이 하나일 경우 String으로 변환
            if (predicateDef.parsedArgs.size == 1) {
                ReflectionUtil.createInstanceOfType(predicateClass, predicateDef.parsedArgs[0])
            } else {
                ReflectionUtil.createInstanceOfType(predicateClass, *predicateDef.parsedArgs)
            }
        } catch (e: Exception) {
            handlePredicateInstantiationError(
                e,
                routeId,
                predicateDef.name,
                predicateClass,
                predicateDef.parsedArgs
            )
        }
        
        // Cache the created instance for future use
        predicateInstanceCache[cacheKey] = predicate
        log.debug { "Cached new predicate instance: $cacheKey" }
        
        return predicate
    }
    
    /**
     * Predicate 캐시 키를 생성합니다.
     * 형식: "PredicateName:arg1,arg2,..."
     */
    private fun createPredicateCacheKey(predicateDef: PredicateDefinition): String {
        val argsString = predicateDef.parsedArgs.joinToString(",")
        return "${predicateDef.name}:$argsString"
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