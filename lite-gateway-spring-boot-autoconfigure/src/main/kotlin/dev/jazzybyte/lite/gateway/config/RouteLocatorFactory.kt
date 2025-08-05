package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import dev.jazzybyte.lite.gateway.route.RouteLocator
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import dev.jazzybyte.lite.gateway.route.StaticRouteLocator
import dev.jazzybyte.lite.gateway.util.ReflectionUtil
import dev.jazzybyte.lite.gateway.exception.PredicateInstantiationException
import dev.jazzybyte.lite.gateway.exception.PredicateDiscoveryException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import java.lang.reflect.Constructor

private val log = KotlinLogging.logger {}

class RouteLocatorFactory {

    companion object {

        // Predicate 클래스들을 동적으로 로드하기 위한 맵(Predicate Prefix -> 클래스)
        private val predicateClasses: Map<String, Class<out RoutePredicate>> = initializePredicateClasses()

        /**
         * Predicate 클래스들을 초기화하고 검증하는 함수
         * 패키지 스캔 결과를 검증하고 중복 Predicate 이름을 처리합니다.
         */
        private fun initializePredicateClasses(): Map<String, Class<out RoutePredicate>> {
            val packageName = "dev.jazzybyte.lite.gateway.route"
            
            try {
                log.debug { "Starting predicate discovery in package: $packageName" }
                
                // 패키지 스캔을 통해 RoutePredicate 구현체들을 찾음
                val discoveredClasses = ReflectionUtil.findClassesOfType(packageName, RoutePredicate::class.java)
                
                // 패키지 스캔 결과 검증
                validatePackageScanResults(discoveredClasses, packageName)
                
                // Predicate 이름과 클래스 매핑 생성 및 중복 검증
                val predicateMap = buildPredicateMap(discoveredClasses)
                
                log.info { "Successfully initialized ${predicateMap.size} predicate classes: ${predicateMap.keys}" }
                log.debug { "Predicate class mappings: $predicateMap" }
                
                return predicateMap
                
            } catch (e: Exception) {
                val errorMessage = "Failed to discover predicate classes in package '$packageName'"
                log.error(e) { errorMessage }
                
                throw PredicateDiscoveryException(
                    message = errorMessage,
                    packageName = packageName,
                    cause = e
                )
            }
        }

        /**
         * 패키지 스캔 결과를 검증합니다.
         */
        private fun validatePackageScanResults(
            discoveredClasses: List<Class<out RoutePredicate>>,
            packageName: String
        ) {
            if (discoveredClasses.isEmpty()) {
                throw PredicateDiscoveryException(
                    message = "No predicate classes found during package scan. " +
                            "Ensure that predicate classes exist and implement RoutePredicate interface.",
                    packageName = packageName
                )
            }
            
            log.debug { "Found ${discoveredClasses.size} predicate classes: ${discoveredClasses.map { it.simpleName }}" }
            
            // 각 클래스가 올바르게 로드되었는지 검증
            discoveredClasses.forEach { predicateClass ->
                try {
                    // 클래스 로딩 검증 - 클래스가 실제로 접근 가능한지 확인
                    predicateClass.constructors
                    predicateClass.simpleName
                } catch (e: Exception) {
                    throw PredicateDiscoveryException(
                        message = "Failed to load predicate class '${predicateClass.name}'. " +
                                "Class may be corrupted or have accessibility issues.",
                        predicateName = predicateClass.simpleName,
                        packageName = packageName,
                        cause = e
                    )
                }
            }
        }

        /**
         * Predicate 클래스들로부터 이름-클래스 매핑을 생성하고 중복을 처리합니다.
         */
        private fun buildPredicateMap(
            discoveredClasses: List<Class<out RoutePredicate>>
        ): Map<String, Class<out RoutePredicate>> {
            val predicateMap = mutableMapOf<String, Class<out RoutePredicate>>()
            val duplicateNames = mutableSetOf<String>()
            
            discoveredClasses.forEach { predicateClass ->
                val predicateName = predicateClass.simpleName.removeSuffix("Predicate")
                
                // 빈 이름 검증
                if (predicateName.isBlank()) {
                    throw PredicateDiscoveryException(
                        message = "Predicate class '${predicateClass.name}' has invalid name. " +
                                "Class name should follow the pattern '*Predicate' where * is not empty.",
                        predicateName = predicateClass.simpleName
                    )
                }
                
                // 중복 이름 검증
                if (predicateMap.containsKey(predicateName)) {
                    duplicateNames.add(predicateName)
                    val existingClass = predicateMap[predicateName]!!
                    
                    log.warn { 
                        "Duplicate predicate name '$predicateName' found. " +
                        "Classes: '${existingClass.name}' and '${predicateClass.name}'. " +
                        "Using the first discovered class: '${existingClass.name}'"
                    }
                } else {
                    predicateMap[predicateName] = predicateClass
                    log.debug { "Registered predicate: '$predicateName' -> '${predicateClass.name}'" }
                }
            }
            
            // 중복 이름이 발견된 경우 경고 로그 출력
            if (duplicateNames.isNotEmpty()) {
                log.warn { 
                    "Found ${duplicateNames.size} duplicate predicate names: $duplicateNames. " +
                    "Only the first discovered class for each name will be used. " +
                    "Consider renaming predicate classes to avoid conflicts."
                }
            }
            
            return predicateMap.toMap()
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
                    ?: throw PredicateDiscoveryException(
                        message = "Unknown predicate '${predicateDef.name}' in route definition with ID '${def.id}'. " +
                                "Available predicates: ${predicateClasses.keys}",
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
