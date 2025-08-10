package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * 라우트 생성 과정의 로깅 및 성능 메트릭을 담당하는 클래스입니다.
 */
class RouteCreationLogger {

    /**
     * 라우트 생성 시작을 로깅합니다.
     */
    fun logRouteCreationStart(routeCount: Int) {
        log.info { "Starting route creation process for $routeCount route definitions" }
    }

    /**
     * 개별 라우트 생성 진행 상황을 로깅합니다.
     */
    fun logRouteCreationProgress(routeId: String, currentIndex: Int, totalCount: Int) {
        log.debug { "Creating route $currentIndex/$totalCount: '$routeId'" }
    }

    /**
     * 라우트 생성 성공을 로깅합니다.
     */
    fun logRouteCreationSuccess(
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

    /**
     * Predicate 매핑 정보를 로깅합니다.
     */
    fun logPredicateMappings(def: RouteDefinition, predicates: List<RoutePredicate>, predicateRegistry: PredicateRegistry) {
        if (predicates.isNotEmpty()) {
            val predicateInfo = def.predicates.mapIndexed { pIndex, pDef ->
                val predicateClass = predicateRegistry.getPredicateClass(pDef.name)
                "predicate_${pIndex + 1}=[name='${pDef.name}', args='${pDef.args}', class='${predicateClass?.simpleName}']"
            }.joinToString(", ")

            log.debug { "Route '${def.id}' predicate mappings: $predicateInfo" }
        }
    }

    /**
     * 라우트 생성 실패를 로깅합니다.
     */
    fun logRouteCreationFailure(def: RouteDefinition, creationTime: Long, exception: Exception) {
        log.error(exception) {
            "Failed to create route '${def.id}' after ${creationTime}ms. " +
            "Route definition: [id='${def.id}', uri='${def.uri}', " +
            "predicates=${def.predicates.map { "[name='${it.name}', args='${it.args}']" }}, " +
            "filters=${def.filters.map { "[name='${it.name}', args='${it.args}']" }}, " +
            "order=${def.order}]. " +
            "Error: ${exception.message}"
        }
    }

    /**
     * 전체 라우트 생성 완료를 로깅합니다.
     */
    fun logRouteCreationCompletion(routes: List<Route>, totalCreationTime: Long) {
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

    /**
     * 성능 메트릭을 계산합니다.
     */
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

    /**
     * 라우트 요약 정보를 로깅합니다.
     */
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

    /**
     * 성능 메트릭 데이터 클래스
     */
    private data class PerformanceMetrics(
        val totalRoutes: Int,
        val totalPredicates: Int,
        val orderRange: String,
        val totalCreationTime: Long,
        val avgRouteCreationTime: Long
    )
}