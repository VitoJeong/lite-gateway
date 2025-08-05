package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.exception.PredicateDiscoveryException
import dev.jazzybyte.lite.gateway.exception.PredicateInstantiationException
import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import dev.jazzybyte.lite.gateway.route.RouteLocator
import dev.jazzybyte.lite.gateway.route.StaticRouteLocator
import dev.jazzybyte.lite.gateway.util.ReflectionUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

private val log = KotlinLogging.logger {}

/**
 * RouteLocatorFactory는 RouteLocator를 생성하는 팩토리 클래스입니다.
 * 이 클래스는 Predicate 클래스를 동적으로 로드하고, 라우트 정의를 기반으로 라우트를 생성합니다.
 * 또한, Predicate 클래스의 검색 및 로딩 과정에서 발생할 수 있는 오류를 처리합니다.
 */
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
            val discoveryStartTime = System.currentTimeMillis()
            
            try {
                log.info { "Starting predicate discovery in package: $packageName" }
                
                // 패키지 스캔을 통해 RoutePredicate 구현체들을 찾음
                val discoveredClasses = ReflectionUtil.findClassesOfType(packageName, RoutePredicate::class.java)
                
                // 패키지 스캔 결과 검증
                validatePackageScanResults(discoveredClasses, packageName)
                
                // Predicate 이름과 클래스 매핑 생성 및 중복 검증
                val predicateMap = buildPredicateMap(discoveredClasses)
                
                val discoveryTime = System.currentTimeMillis() - discoveryStartTime
                
                // Predicate 클래스 발견 시 이름과 클래스 매핑을 구조화된 형태로 로깅 (요구사항 5.2)
                log.info { 
                    "Predicate discovery completed successfully: " +
                    "discovered_classes=${discoveredClasses.size}, " +
                    "registered_predicates=${predicateMap.size}, " +
                    "discovery_time=${discoveryTime}ms"
                }
                
                // 각 Predicate 이름과 클래스 매핑의 상세 로깅
                predicateMap.forEach { (name, clazz) ->
                    log.info { "Registered predicate mapping: name='$name' -> class='${clazz.name}'" }
                }
                
                // 디버그 레벨에서 추가 상세 정보 제공
                if (log.isDebugEnabled()) {
                    log.debug { "Complete predicate class mappings: $predicateMap" }
                    
                    // 각 클래스의 생성자 정보도 로깅
                    predicateMap.forEach { (name, clazz) ->
                        val constructors = clazz.constructors.map { constructor ->
                            val paramTypes = constructor.parameterTypes.joinToString(", ") { it.simpleName }
                            "($paramTypes)"
                        }
                        log.debug { "Predicate '$name' available constructors: ${constructors.joinToString(", ")}" }
                    }
                }
                
                return predicateMap
                
            } catch (e: Exception) {
                val discoveryTime = System.currentTimeMillis() - discoveryStartTime
                val errorMessage = "Failed to discover predicate classes in package '$packageName' after ${discoveryTime}ms"
                
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
            val startTime = System.currentTimeMillis()
            logRouteCreationStart(routeDefinitions.size)

            val routes = createRoutes(routeDefinitions)
                .sortedBy { it.order }

            val totalCreationTime = System.currentTimeMillis() - startTime
            logRouteCreationCompletion(routes, totalCreationTime)

            return StaticRouteLocator(routes)
        }

        private fun createRoutes(routeDefinitions: List<RouteDefinition>): List<Route> {
            return routeDefinitions.mapIndexed { index, def ->
                createSingleRoute(def, index + 1, routeDefinitions.size)
            }
        }

        private fun createSingleRoute(def: RouteDefinition, currentIndex: Int, totalCount: Int): Route {
            val routeStartTime = System.currentTimeMillis()

            try {
                logRouteCreationProgress(def.id, currentIndex, totalCount)

                // 라우트 정의 검증
                validateRouteDefinition(def)

                val predicates = initPredicates(def)
                val route = buildRoute(def, predicates)
                val routeCreationTime = System.currentTimeMillis() - routeStartTime

                logRouteCreationSuccess(def, predicates, route, routeCreationTime)
                logPredicateMappings(def, predicates)

                return route

            } catch (e: RouteConfigurationException) {
                val routeCreationTime = System.currentTimeMillis() - routeStartTime
                logRouteCreationFailure(def, routeCreationTime, e)
                // RouteConfigurationException은 그대로 전파
                throw e
            } catch (e: PredicateDiscoveryException) {
                val routeCreationTime = System.currentTimeMillis() - routeStartTime
                logRouteCreationFailure(def, routeCreationTime, e)
                // Predicate 관련 예외는 원본 그대로 전파 (기존 테스트 호환성 유지)
                throw e
            } catch (e: PredicateInstantiationException) {
                val routeCreationTime = System.currentTimeMillis() - routeStartTime
                logRouteCreationFailure(def, routeCreationTime, e)
                // Predicate 인스턴스화 예외는 원본 그대로 전파 (기존 테스트 호환성 유지)
                throw e
            } catch (e: Exception) {
                val routeCreationTime = System.currentTimeMillis() - routeStartTime
                logRouteCreationFailure(def, routeCreationTime, e)
                // 기타 예외를 RouteConfigurationException으로 래핑
                throw RouteConfigurationException(
                    message = "Unexpected error during route creation: ${e.message}",
                    routeId = def.id,
                    cause = e
                )
            }
        }

        private fun buildRoute(def: RouteDefinition, predicates: List<RoutePredicate>): Route {
            val order = validateAndNormalizeOrder(def.id, def.order)
            return Route(
                id = def.id,
                uri = def.uri,
                predicates = predicates,
                order = order
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

            // Predicate 정의 검증은 initPredicates에서 수행

            // Filter 정의 검증
            def.filters.forEachIndexed { index, filterDef ->
                if (filterDef.name.isBlank()) {
                    throw RouteConfigurationException(
                        message = "Filter name cannot be empty or blank at index $index",
                        routeId = def.id
                    )
                }
            }

            // URI 스키마 검증
            val uriString = def.uri.toString()
            if (!uriString.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) {
                throw RouteConfigurationException(
                    message = "Invalid URI format: '$uriString'. URI must have a valid scheme (e.g., http://, https://)",
                    routeId = def.id
                )
            }

            log.debug { "Route definition validation passed for route '${def.id}'" }
        }

        private fun validateAndNormalizeOrder(routeId: String, order: Int): Int {
            return if (order < 0) {
                log.warn { "Route '$routeId' has negative order $order. Setting to 0." }
                0
            } else {
                order
            }
        }

        private fun logRouteCreationStart(routeCount: Int) {
            log.info { "Starting route creation process for $routeCount route definitions" }
        }

        private fun logRouteCreationProgress(routeId: String, currentIndex: Int, totalCount: Int) {
            log.debug { "Creating route $currentIndex/$totalCount: '$routeId'" }
        }

        private fun logRouteCreationSuccess(
            def: RouteDefinition,
            predicates: List<RoutePredicate>,
            route: Route,
            creationTime: Long
        ) {
            log.info {
                "Successfully created route '${def.id}': " +
                "uri='${def.uri}', " +
                "predicates=${predicates.size}, " +
                "order=${route.order}, " +
                "creation_time=${creationTime}ms"
            }
        }

        private fun logPredicateMappings(def: RouteDefinition, predicates: List<RoutePredicate>) {
            if (predicates.isNotEmpty()) {
                val predicateInfo = def.predicates.mapIndexed { pIndex, pDef ->
                    "predicate_${pIndex + 1}=[name='${pDef.name}', args='${pDef.args}', class='${predicateClasses[pDef.name]?.simpleName}']"
                }.joinToString(", ")

                log.debug { "Route '${def.id}' predicate mappings: $predicateInfo" }
            }
        }

        private fun logRouteCreationFailure(def: RouteDefinition, creationTime: Long, exception: Exception) {
            log.error(exception) {
                "Failed to create route '${def.id}' after ${creationTime}ms. " +
                "Route definition: [id='${def.id}', uri='${def.uri}', " +
                "predicates=${def.predicates.map { "[name='${it.name}', args='${it.args}']" }}, " +
                "filters=${def.filters.map { "[name='${it.name}', args='${it.args}']" }}, " +
                "order=${def.order}]. " +
                "Error: ${exception.message}"
            }
        }

        private fun logRouteCreationCompletion(routes: List<Route>, totalCreationTime: Long) {
            val performanceMetrics = calculatePerformanceMetrics(routes, totalCreationTime)
            
            log.info {
                "Route creation completed successfully: " +
                "total_routes=${performanceMetrics.totalRoutes}, " +
                "total_predicates=${performanceMetrics.totalPredicates}, " +
                "order_range=[${performanceMetrics.orderRange}], " +
                "total_creation_time=${performanceMetrics.totalCreationTime}ms, " +
                "avg_route_creation_time=${performanceMetrics.avgRouteCreationTime}ms"
            }

            logRouteSummaries(routes)
        }

        private fun calculatePerformanceMetrics(routes: List<Route>, totalCreationTime: Long): PerformanceMetrics {
            val totalPredicates = routes.sumOf { route -> route.predicates.size }
            val orderRange = if (routes.isNotEmpty()) "${routes.first().order}..${routes.last().order}" else "N/A"
            val avgRouteCreationTime = if (routes.isNotEmpty()) totalCreationTime / routes.size else 0

            return PerformanceMetrics(
                totalRoutes = routes.size,
                totalPredicates = totalPredicates,
                orderRange = orderRange,
                totalCreationTime = totalCreationTime,
                avgRouteCreationTime = avgRouteCreationTime
            )
        }

        private fun logRouteSummaries(routes: List<Route>) {
            if (log.isDebugEnabled()) {
                routes.forEach { route ->
                    log.debug {
                        "Route summary: id='${route.id}', uri='${route.uri}', " +
                        "predicates=${route.predicates.size}, order=${route.order}"
                    }
                }
            }
        }

        private data class PerformanceMetrics(
            val totalRoutes: Int,
            val totalPredicates: Int,
            val orderRange: String,
            val totalCreationTime: Long,
            val avgRouteCreationTime: Long
        )

        private fun initPredicates(def: RouteDefinition): List<RoutePredicate> = def.predicates
            .map { predicateDef ->
                // 빈 Predicate 이름 검증
                if (predicateDef.name.isBlank()) {
                    throw PredicateDiscoveryException(
                        message = "Unknown predicate '${predicateDef.name}' in route definition with ID '${def.id}'. " +
                                "Available predicates: ${predicateClasses.keys}",
                        predicateName = predicateDef.name
                    )
                }
                
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
