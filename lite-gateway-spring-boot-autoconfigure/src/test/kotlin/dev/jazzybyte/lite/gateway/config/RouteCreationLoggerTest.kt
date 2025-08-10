package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import dev.jazzybyte.lite.gateway.route.Route
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import dev.jazzybyte.lite.gateway.route.PredicateDefinition
import dev.jazzybyte.lite.gateway.context.RequestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI

@DisplayName("RouteCreationLogger 테스트")
class RouteCreationLoggerTest {

    private lateinit var logger: RouteCreationLogger
    private lateinit var predicateRegistry: PredicateRegistry

    @BeforeEach
    fun setUp() {
        logger = RouteCreationLogger()
        predicateRegistry = PredicateRegistry()
    }

    @Test
    @DisplayName("라우트 생성 시작 로깅이 정상적으로 작동해야 함")
    fun `should log route creation start successfully`() {
        // when & then - 예외가 발생하지 않아야 함
        logger.logRouteCreationStart(5)
    }

    @Test
    @DisplayName("라우트 생성 진행 상황 로깅이 정상적으로 작동해야 함")
    fun `should log route creation progress successfully`() {
        // when & then - 예외가 발생하지 않아야 함
        logger.logRouteCreationProgress("test-route", 1, 5)
    }

    @Test
    @DisplayName("라우트 생성 성공 로깅이 정상적으로 작동해야 함")
    fun `should log route creation success successfully`() {
        // given
        val routeDefinition = RouteDefinition(
            id = "test-route",
            uri = "http://example.com",
            predicates = listOf(PredicateDefinition(name = "Test", args = "test-arg")),
            order = 1
        )
        
        val predicates = listOf(TestPredicate())
        val route = Route(
            id = "test-route",
            uri = "http://example.com",
            order = 1,
            predicates = predicates
        )

        // when & then - 예외가 발생하지 않아야 함
        logger.logRouteCreationSuccess(routeDefinition, predicates, route, 100L)
    }

    @Test
    @DisplayName("Predicate 매핑 로깅이 정상적으로 작동해야 함")
    fun `should log predicate mappings successfully`() {
        // given
        val routeDefinition = RouteDefinition(
            id = "test-route",
            uri = "http://example.com",
            predicates = listOf(PredicateDefinition(name = "Test", args = "test-arg")),
            order = 1
        )
        
        val predicates = listOf(TestPredicate())

        // when & then - 예외가 발생하지 않아야 함
        logger.logPredicateMappings(routeDefinition, predicates, predicateRegistry)
    }

    @Test
    @DisplayName("라우트 생성 실패 로깅이 정상적으로 작동해야 함")
    fun `should log route creation failure successfully`() {
        // given
        val routeDefinition = RouteDefinition(
            id = "test-route",
            uri = "http://example.com",
            predicates = emptyList(),
            order = 1
        )
        
        val exception = RuntimeException("Test exception")

        // when & then - 예외가 발생하지 않아야 함
        logger.logRouteCreationFailure(routeDefinition, 100L, exception)
    }

    @Test
    @DisplayName("라우트 생성 완료 로깅이 정상적으로 작동해야 함")
    fun `should log route creation completion successfully`() {
        // given
        val routes = listOf(
            Route(
                id = "route1",
                uri = "http://example1.com",
                order = 1,
                predicates = listOf(TestPredicate())
            ),
            Route(
                id = "route2",
                uri = "http://example2.com",
                order = 2,
                predicates = listOf(TestPredicate())
            )
        )

        // when & then - 예외가 발생하지 않아야 함
        logger.logRouteCreationCompletion(routes, 500L)
    }

    // 테스트용 Predicate 구현
    private class TestPredicate : RoutePredicate {
        override fun matches(context: RequestContext): Boolean = true
    }
}