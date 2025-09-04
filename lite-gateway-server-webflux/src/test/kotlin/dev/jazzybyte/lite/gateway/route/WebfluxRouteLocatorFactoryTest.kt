package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.exception.PredicateDiscoveryException
import dev.jazzybyte.lite.gateway.exception.PredicateInstantiationException
import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import dev.jazzybyte.lite.gateway.filter.GatewayFilterFactory
import io.mockk.mockk

/**
 * RouteLocatorFactory에 대한 포괄적인 단위 테스트
 * 
 * 테스트 범위:
 * - 정상적인 라우트 생성 시나리오
 * - Predicate 발견 로직의 엣지 케이스
 * - 오류 처리 시나리오
 * - 다양한 설정 조합
 */
@DisplayName("RouteLocatorFactory 테스트")
class WebfluxRouteLocatorFactoryTest {
    
    val gatewayFilterFactory = mockk<GatewayFilterFactory>()
    val routeLocatorFactory = WebfluxRouteLocatorFactory(gatewayFilterFactory)

    @Nested
    @DisplayName("정상적인 라우트 생성")
    inner class SuccessfulRouteCreation {

        @Test
        @DisplayName("단일 라우트 생성 - 기본 설정")
        fun `should create single route with basic configuration`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "test-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/api/**")
                    ),
                    order = 1
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            assertThat(routeLocator).isInstanceOf(StaticRouteLocator::class.java)
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(1)
            
            val route = staticRouteLocator.routes[0]
            assertThat(route.id).isEqualTo("test-route")
            assertThat(route.uri.toString()).isEqualTo("http://example.com:80")
            assertThat(route.predicates).hasSize(1)
            assertThat(route.predicates[0]).isInstanceOf(PathPredicate::class.java)
        }

        @Test
        @DisplayName("다중 라우트 생성 - order 정렬 확인")
        fun `should create multiple routes and sort by order`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "route-3",
                    uri = "http://example3.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/api3/**")),
                    order = 3
                ),
                RouteDefinition(
                    id = "route-1",
                    uri = "http://example1.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/api1/**")),
                    order = 1
                ),
                RouteDefinition(
                    id = "route-2",
                    uri = "http://example2.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/api2/**")),
                    order = 2
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(3)
            
            // order에 따라 정렬되었는지 확인
            assertThat(staticRouteLocator.routes[0].id).isEqualTo("route-1")
            assertThat(staticRouteLocator.routes[1].id).isEqualTo("route-2")
            assertThat(staticRouteLocator.routes[2].id).isEqualTo("route-3")
        }

        @Test
        @DisplayName("다중 Predicate를 가진 라우트 생성")
        fun `should create route with multiple predicates`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "multi-predicate-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/api/**"),
                        PredicateDefinition(name = "Method", args = "GET"),
                        PredicateDefinition(name = "Host", args = "example.com")
                    )
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            val route = staticRouteLocator.routes[0]
            assertThat(route.predicates).hasSize(3)
            assertThat(route.predicates[0]).isInstanceOf(PathPredicate::class.java)
            assertThat(route.predicates[1]).isInstanceOf(MethodPredicate::class.java)
            // Host predicate는 단일 인수를 받으므로 정상 작동
            val predicateTypes = route.predicates.map { it::class.java.simpleName }
            assertThat(predicateTypes).contains("HostPredicate")
        }

        @Test
        @DisplayName("빈 라우트 정의 목록 처리")
        fun `should handle empty route definitions list`() {
            // given
            val routeDefinitions = mutableListOf<RouteDefinition>()

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).isEmpty()
        }

        @Test
        @DisplayName("빈 Predicate 목록을 가진 라우트 생성")
        fun `should create route with empty predicates list`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "no-predicate-route",
                    uri = "http://example.com",
                    predicates = emptyList()
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            val route = staticRouteLocator.routes[0]
            assertThat(route.predicates).isEmpty()
        }
    }

    @Nested
    @DisplayName("Predicate 인수 처리")
    inner class PredicateArgumentHandling {

        @Test
        @DisplayName("단일 인수를 가진 Predicate 생성")
        fun `should create predicate with single argument`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "single-arg-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/api/**")
                    )
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            val route = staticRouteLocator.routes[0]
            assertThat(route.predicates).hasSize(1)
            assertThat(route.predicates[0]).isInstanceOf(PathPredicate::class.java)
        }

        @Test
        @DisplayName("다중 인수를 가진 Cookie Predicate 생성")
        fun `should create predicate with multiple arguments`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "multi-arg-cookie-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Cookie", args = "session,abc123")
                    )
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            val route = staticRouteLocator.routes[0]
            assertThat(route.predicates).hasSize(1)
            assertThat(route.predicates[0]).isInstanceOf(CookiePredicate::class.java)
        }

        @Test
        @DisplayName("빈 인수를 가진 MethodPredicate 생성 시 예외 발생")
        fun `should throw exception for MethodPredicate with empty arguments`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "empty-arg-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Method", args = "")
                    )
                )
            )

            // when & then
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(PredicateInstantiationException::class.java)
        }
        @Test
        @DisplayName("빈 인수를 가진 HeaderPredicate 생성 시 예외 발생")
        fun `should throw exception for HeaderPredicate with empty arguments`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "empty-arg-header-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Header", args = "")
                    )
                )
            )

            // when & then
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(PredicateInstantiationException::class.java)
        }
    }

    @Nested
    @DisplayName("오류 처리 시나리오")
    inner class ErrorHandlingScenarios {

        @Test
        @DisplayName("알 수 없는 Predicate 이름으로 예외 발생")
        fun `should throw exception for unknown predicate name`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "unknown-predicate-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "UnknownPredicate", args = "test")
                    )
                )
            )

            // when & then
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(PredicateDiscoveryException::class.java)
                .hasMessageContaining("Unknown predicate 'UnknownPredicate'")
                .hasMessageContaining("route definition with ID 'unknown-predicate-route'")
                .hasMessageContaining("Available predicates:")
        }

        @Test
        @DisplayName("Predicate 인스턴스화 실패로 예외 발생")
        fun `should throw exception when predicate instantiation fails`() {
            // given - 잘못된 인수 개수로 Predicate 생성 시도
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "invalid-args-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        // MethodPredicate는 단일 String 인수를 받지만 여러 인수를 전달
                        PredicateDefinition(name = "Method", args = "GET, POST, PUT")
                    )
                )
            )

            // when & then
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(PredicateInstantiationException::class.java)
                .hasMessageContaining("Failed to instantiate predicate 'Method' for route 'invalid-args-route'")
                .hasMessageContaining("Constructor matching failed")
        }

        @Test
        @DisplayName("Predicate 인스턴스화 실패 시 상세한 디버깅 정보 제공")
        fun `should provide detailed debugging information when predicate instantiation fails`() {
            // given - 잘못된 인수 타입으로 Predicate 생성 시도
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "detailed-error-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        // MethodPredicate는 단일 String 인수를 받지만 여러 인수를 전달
                        PredicateDefinition(name = "Method", args = "GET, POST, PUT")
                    )
                )
            )

            // when & then
            try {
                routeLocatorFactory.create(routeDefinitions)
                fail("Expected PredicateInstantiationException to be thrown")
            } catch (exception: PredicateInstantiationException) {
                assertThat(exception.routeId).isEqualTo("detailed-error-route")
                assertThat(exception.predicateName).isEqualTo("Method")
                assertThat(exception.predicateArgs).containsExactly("GET", "POST", "PUT")
                assertThat(exception.message)
                    .contains("Constructor matching failed")
                    .contains("Available constructors")
                    .contains("Provided argument types")
            }
        }

        @Test
        @DisplayName("null Predicate 이름으로 예외 발생")
        fun `should throw exception for null predicate name`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "null-predicate-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "", args = "test")
                    )
                )
            )

            // when & then
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(PredicateDiscoveryException::class.java)
                .hasMessageContaining("Unknown predicate ''")
                .hasMessageContaining("route definition with ID 'null-predicate-route'")
                .hasMessageContaining("Available predicates:")
        }
    }



    @Nested
    @DisplayName("복잡한 시나리오")
    inner class ComplexScenarios {

        @Test
        @DisplayName("동일한 order를 가진 라우트들의 처리 - 예외 발생")
        fun `should throw exception for routes with duplicate order values`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "route-a",
                    uri = "http://example-a.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/a/**")),
                    order = 1
                ),
                RouteDefinition(
                    id = "route-b",
                    uri = "http://example-b.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/b/**")),
                    order = 1
                )
            )

            // when & then
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(RouteConfigurationException::class.java)
                .hasMessageContaining("Duplicate order values found in route definitions")
                .hasMessageContaining("order 1: [route-a, route-b]")
        }

        @Test
        @DisplayName("매우 큰 order 값 처리")
        fun `should handle very large order values`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "large-order-route",
                    uri = "http://example.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/api/**")),
                    order = Int.MAX_VALUE
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(1)
            assertThat(staticRouteLocator.routes[0].order).isEqualTo(Int.MAX_VALUE)
        }

        @Test
        @DisplayName("음수 order 값 처리 - 0으로 치환됨")
        fun `should handle negative order values replace to zero`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "negative-order-route",
                    uri = "http://example.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/api/**")),
                    order = -1
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(1)
            // 현재 구현에서는 order 파라미터가 0으로 치환됨
            assertThat(staticRouteLocator.routes[0].order).isEqualTo(0)
        }

        @Test
        @DisplayName("특수 문자가 포함된 URI 처리")
        fun `should handle URIs with special characters`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "special-uri-route",
                    uri = "http://example.com:8080/path?param=value&other=test",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/api/**"))
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            val route = staticRouteLocator.routes[0]
            assertThat(route.uri.toString()).isEqualTo("http://example.com:8080/path?param=value&other=test")
        }

        @Test
        @DisplayName("다중 중복 order 값 검증")
        fun `should throw exception for multiple duplicate order values`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "route-1a",
                    uri = "http://example1a.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/1a/**")),
                    order = 1
                ),
                RouteDefinition(
                    id = "route-1b",
                    uri = "http://example1b.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/1b/**")),
                    order = 1
                ),
                RouteDefinition(
                    id = "route-2a",
                    uri = "http://example2a.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/2a/**")),
                    order = 2
                ),
                RouteDefinition(
                    id = "route-2b",
                    uri = "http://example2b.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/2b/**")),
                    order = 2
                ),
                RouteDefinition(
                    id = "route-3",
                    uri = "http://example3.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/3/**")),
                    order = 3
                )
            )

            // when & then
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(RouteConfigurationException::class.java)
                .hasMessageContaining("Duplicate order values found in route definitions")
                .hasMessageContaining("order 1: [route-1a, route-1b]")
                .hasMessageContaining("order 2: [route-2a, route-2b]")
        }

        @Test
        @DisplayName("세 개 이상의 라우트가 동일한 order를 가진 경우")
        fun `should throw exception for three or more routes with same order`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "route-a",
                    uri = "http://example-a.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/a/**")),
                    order = 5
                ),
                RouteDefinition(
                    id = "route-b",
                    uri = "http://example-b.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/b/**")),
                    order = 5
                ),
                RouteDefinition(
                    id = "route-c",
                    uri = "http://example-c.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/c/**")),
                    order = 5
                )
            )

            // when & then
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(RouteConfigurationException::class.java)
                .hasMessageContaining("Duplicate order values found in route definitions")
                .hasMessageContaining("order 5: [route-a, route-b, route-c]")
        }

        @Test
        @DisplayName("고유한 order 값들만 있는 경우 정상 처리")
        fun `should handle routes with unique order values successfully`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "route-1",
                    uri = "http://example1.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/1/**")),
                    order = 1
                ),
                RouteDefinition(
                    id = "route-2",
                    uri = "http://example2.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/2/**")),
                    order = 2
                ),
                RouteDefinition(
                    id = "route-3",
                    uri = "http://example3.com",
                    predicates = listOf(PredicateDefinition(name = "Path", args = "/3/**")),
                    order = 3
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then - 예외 없이 성공적으로 생성되어야 함
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(3)
            assertThat(staticRouteLocator.routes[0].id).isEqualTo("route-1")
            assertThat(staticRouteLocator.routes[1].id).isEqualTo("route-2")
            assertThat(staticRouteLocator.routes[2].id).isEqualTo("route-3")
        }
    }
}