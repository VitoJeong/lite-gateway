package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.config.validation.UriValidator
import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.route.PredicateDefinition
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
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
        predicateRegistry = mockk<PredicateRegistry>()
        uriValidator = mockk<UriValidator>()
        routeBuilder = RouteBuilder(predicateRegistry, uriValidator)
        
        // UriValidator의 기본 동작 설정 - 정상적인 URI에 대해서는 예외를 발생시키지 않음
        every { uriValidator.validateUriFormat(any(), any()) } just runs
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
        
        // verify - UriValidator가 호출되지 않았는지 확인 (빈 URI는 RouteBuilder에서 먼저 검증)
        verify(exactly = 0) { uriValidator.validateUriFormat(any(), any()) }
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
        
        // verify - UriValidator가 정상적으로 호출되었는지 확인
        verify(exactly = 1) { uriValidator.validateUriFormat("http://example.com", "test-route") }
    }

    @Test
    @DisplayName("UriValidator에서 예외가 발생하면 RouteConfigurationException으로 래핑되어야 함")
    fun `should wrap UriValidator exception as RouteConfigurationException`() {
        // given
        val routeDefinition = RouteDefinition(
            id = "test-route",
            uri = "invalid-uri",
            predicates = emptyList(),
            order = 1
        )
        
        // UriValidator가 예외를 발생시키도록 설정
        every { uriValidator.validateUriFormat("invalid-uri", "test-route") } throws 
            RouteConfigurationException("Invalid URI format", "test-route")

        // when & then
        val exception = assertThrows<RouteConfigurationException> {
            routeBuilder.buildRoute(routeDefinition)
        }
        
        assertThat(exception.message).contains("Invalid URI format")
        assertThat(exception.routeId).isEqualTo("test-route")
        
        // verify - UriValidator가 호출되었는지 확인
        verify(exactly = 1) { uriValidator.validateUriFormat("invalid-uri", "test-route") }
    }

    @Test
    @DisplayName("빈 필터 이름을 가진 RouteDefinition은 검증 실패해야 함")
    fun `should fail validation for RouteDefinition with empty filter name`() {
        // given
        val routeDefinition = RouteDefinition(
            id = "test-route",
            uri = "http://example.com",
            predicates = emptyList(),
            filters = mutableListOf(
                FilterDefinition(name = "", args = mapOf("key" to "value"))
            ),
            order = 1
        )

        // when & then
        val exception = assertThrows<RouteConfigurationException> {
            routeBuilder.buildRoute(routeDefinition)
        }
        
        assertThat(exception.message).contains("Filter name cannot be empty")
        assertThat(exception.routeId).isEqualTo("test-route")
    }
}