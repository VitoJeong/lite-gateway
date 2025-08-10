package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.config.validation.UriValidator
import dev.jazzybyte.lite.gateway.exception.PredicateDiscoveryException
import dev.jazzybyte.lite.gateway.exception.PredicateInstantiationException
import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import dev.jazzybyte.lite.gateway.route.RouteLocator
import dev.jazzybyte.lite.gateway.route.StaticRouteLocator
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

private val log = KotlinLogging.logger {}

/**
 * RouteLocatorFactory는 RouteLocator를 생성하는 팩토리 클래스입니다.
 * 이 클래스는 여러 전문화된 컴포넌트들을 조율하여 라우트 정의를 기반으로 라우트를 생성합니다.
 * 
 * 주요 컴포넌트:
 * - PredicateRegistry: Predicate 클래스 발견 및 관리
 * - RouteBuilder: 라우트 생성 로직
 * - UriValidator: URI 검증 로직
 */
class RouteLocatorFactory {

    companion object {
        // 컴포넌트 인스턴스들
        private val predicateRegistry = PredicateRegistry()
        private val uriValidator = UriValidator()
        private val routeBuilder = RouteBuilder(predicateRegistry, uriValidator)

        /**
         * RouteDefinition 목록으로부터 RouteLocator를 생성합니다.
         */
        fun create(routeDefinitions: @NotNull @Valid MutableList<RouteDefinition>): RouteLocator {
            val startTime = System.currentTimeMillis()
            log.info { "Starting route creation process for ${routeDefinitions.size} route definitions" }

            val routes = createRoutes(routeDefinitions)
                .sortedBy { it.order }

            // 중복 order 값 검증
            validateRouteOrders(routeDefinitions)

            val totalCreationTime = System.currentTimeMillis() - startTime
            logRouteCreationCompletion(routes, totalCreationTime)

            return StaticRouteLocator(routes)
        }

        /**
         * RouteDefinition 목록으로부터 Route 목록을 생성합니다.
         */
        private fun createRoutes(routeDefinitions: List<RouteDefinition>): List<Route> {
            return routeDefinitions.mapIndexed { index, def ->
                createSingleRoute(def, index + 1, routeDefinitions.size)
            }
        }

        /**
         * 단일 RouteDefinition으로부터 Route를 생성합니다.
         */
        private fun createSingleRoute(def: RouteDefinition, currentIndex: Int, totalCount: Int): Route {
            val routeStartTime = System.currentTimeMillis()

            try {
                log.debug { "Creating route $currentIndex/$totalCount: '${def.id}'" }

                // RouteBuilder를 사용하여 라우트 생성
                val route = routeBuilder.buildRoute(def)
                val routeCreationTime = System.currentTimeMillis() - routeStartTime

                log.info {
                    "Successfully created route '${def.id}': " +
                    "uri='${def.uri}', " +
                    "predicates=${route.predicates.size}, " +
                    "order=${route.order}, " +
                    "creation_time=${routeCreationTime}ms"
                }

                logPredicateMappings(def, route, predicateRegistry)

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

        /**
         * 라우트 생성 완료를 로깅합니다.
         */
        private fun logRouteCreationCompletion(routes: List<Route>, totalCreationTime: Long) {
            val totalPredicates = routes.sumOf { it.predicates.size }
            val orderRange = if (routes.isNotEmpty()) {
                "${routes.minOf { it.order }}..${routes.maxOf { it.order }}"
            } else {
                "N/A"
            }
            val avgCreationTime = if (routes.isNotEmpty()) totalCreationTime / routes.size else 0
            
            log.info { 
                "Route creation completed successfully: " +
                "total_routes=${routes.size}, " +
                "total_predicates=$totalPredicates, " +
                "order_range=[$orderRange], " +
                "total_creation_time=${totalCreationTime}ms, " +
                "avg_route_creation_time=${avgCreationTime}ms" 
            }
            
            // 각 라우트 요약 정보 로깅
            routes.forEach { route ->
                log.debug { "Route summary: id='${route.id}', uri='${route.uri}', predicates=${route.predicates.size}, order=${route.order}" }
            }
        }

        /**
         * Predicate 매핑 정보를 로깅합니다.
         */
        private fun logPredicateMappings(def: RouteDefinition, route: Route, predicateRegistry: PredicateRegistry) {
            if (def.predicates.isNotEmpty()) {
                val predicateMappings = def.predicates.mapIndexed { idx, predicate ->
                    val predicateClass = predicateRegistry.getPredicateClass(predicate.name)
                    "predicate_${idx + 1}=[name='${predicate.name}', args='${predicate.args}', class='${predicateClass?.simpleName}']"
                }.joinToString(", ")
                log.debug { "Route '${def.id}' predicate mappings: $predicateMappings" }
            }
        }

        /**
         * 라우트 생성 실패를 로깅합니다.
         */
        private fun logRouteCreationFailure(def: RouteDefinition, routeCreationTime: Long, e: Exception) {
            log.error { 
                "Failed to create route '${def.id}' after ${routeCreationTime}ms. " +
                "Route definition: [id='${def.id}', uri='${def.uri}', predicates=${def.predicates}, filters=${def.filters}, order=${def.order}]. " +
                "Error: ${e.message}" 
            }
        }

        /**
         * 라우트 정의들의 order 값 중복을 검증합니다.
         * 동일한 order 값을 가진 라우트들이 발견되면 RouteConfigurationException을 발생시킵니다.
         *
         * @param routeDefinitions 검증할 라우트 정의 목록
         * @throws RouteConfigurationException 중복된 order 값이 발견된 경우
         */
        private fun validateRouteOrders(routeDefinitions: List<RouteDefinition>) {
            val orderToRouteIds = mutableMapOf<Int, MutableList<String>>()
            
            // order 값별로 라우트 ID들을 그룹화
            routeDefinitions.forEach { route ->
                orderToRouteIds.computeIfAbsent(route.order) { mutableListOf() }.add(route.id)
            }
            
            // 중복된 order 값을 가진 라우트들 찾기
            val duplicateOrders = orderToRouteIds.filter { it.value.size > 1 }
            
            if (duplicateOrders.isNotEmpty()) {
                val conflictDetails = duplicateOrders.map { (order, routeIds) ->
                    "order $order: [${routeIds.joinToString(", ")}]"
                }.joinToString(", ")
                
                throw RouteConfigurationException(
                    message = "Duplicate order values found in route definitions. Routes with same order: $conflictDetails"
                )
            }
        }
    }
}