package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.config.validation.UriValidator
import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import dev.jazzybyte.lite.gateway.route.PredicateDefinition
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("RouteBuilder 테스트")
class RouteBuilderTest {

    private lateinit var predicateRegistry: PredicateRegistry
    private lateinit var uriValidator: UriValidator
    private lateinit var routeBuilder: RouteBuilder

    @BeforeEach
    fun setUp() {
        predicateRegistry = PredicateRegistry()
        uriValidator = UriValidator()
        routeBuilder = RouteBuilder(predicateRegistry, uriValidator)
    }

    @Test
    @DisplayName("유효한 RouteDefinition으로 Route를 생성할 수 있어야 함")
    fun `should build route from valid RouteDefinition`() {
        // given - Predicate 없이 테스트
        val routeDefinition = RouteDefinition(
            id = "test-route",
            uri = "http://example.com",
            predicates = emptyList(), // Predicate 없이 테스트
            order = 1
        )

        // when
        val route = routeBuilder.buildRoute(routeDefinition)

        // then
        assertThat(route.id).isEqualTo("test-route")
        assertThat(route.uri.toString()).isEqualTo("http://example.com:80") // Route 클래스가 기본 포트를 추가함
        assertThat(route.predicates).hasSize(0)
        assertThat(route.order).isEqualTo(1)
    }

    @Test
    @DisplayName("빈 ID를 가진 RouteDefinition은 검증 실패해야 함")
    fun `should fail validation for RouteDefinition with empty ID`() {
        // given
        val routeDefinition = RouteDefinition(
            id = "",
            uri = "http://example.com",
            predicates = emptyList(),
            order = 1
        )

        // when & then
        val exception = assertThrows<RouteConfigurationException> {
            routeBuilder.buildRoute(routeDefinition)
        }
        
        assertThat(exception.message).contains("Route ID cannot be empty")
    }

    @Test
    @DisplayName("빈 URI를 가진 RouteDefinition은 검증 실패해야 함")
    fun `should fail validation for RouteDefinition with empty URI`() {
        // given
        val routeDefinition = RouteDefinition(
            id = "test-route",
            uri = "",
            predicates = emptyList(),
            order = 1
        )

        // when & then
        val exception = assertThrows<RouteConfigurationException> {
            routeBuilder.buildRoute(routeDefinition)
        }
        
        assertThat(exception.message).contains("Route URI cannot be empty")
    }

    @Test
    @DisplayName("음수 order 값은 0으로 정규화되어야 함")
    fun `should normalize negative order value to 0`() {
        // given
        val routeDefinition = RouteDefinition(
            id = "test-route",
            uri = "http://example.com",
            predicates = emptyList(),
            order = -5
        )

        // when
        val route = routeBuilder.buildRoute(routeDefinition)

        // then
        assertThat(route.order).isEqualTo(0)
    }
}