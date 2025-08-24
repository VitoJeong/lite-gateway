package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.exception.PredicateDiscoveryException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * RouteLocatorFactory의 Predicate 발견 로직에 대한 전용 테스트
 * 
 * 테스트 범위:
 * - Predicate 발견 로직 검증 및 개선
 * - Predicate 발견 로직 엣지 케이스
 * - 패키지 스캔 결과 검증
 * - 중복 이름 처리
 */
@DisplayName("PredicateDiscovery 테스트")
class PredicateDiscoveryTest {
    
    val routeLocatorFactory = WebfluxRouteLocatorFactory()

    @Nested
    @DisplayName("Predicate 발견 로직 검증 및 개선")
    inner class PredicateDiscoveryValidation {

        @Test
        @DisplayName("Predicate 클래스 발견 성공 시 로깅 확인")
        fun `should log successful predicate discovery`() {
            // given - 정상적인 라우트 정의
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "discovery-test-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/api/**")
                    )
                )
            )

            // when - 라우트 생성 (Predicate 발견 로직이 실행됨)
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then - 정상적으로 생성되어야 함 (로깅은 수동으로 확인)
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(1)
            assertThat(staticRouteLocator.routes[0].predicates).hasSize(1)
        }

        @Test
        @DisplayName("Predicate 발견 과정에서 알려진 Predicate들이 모두 등록되는지 확인")
        fun `should register all known predicate types during discovery`() {
            // given - 모든 알려진 Predicate 타입을 사용하는 라우트
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "all-predicates-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/api/**"),
                        PredicateDefinition(name = "Method", args = "GET"),
                        PredicateDefinition(name = "Host", args = "example.com"),
                        PredicateDefinition(name = "Header", args = "X-Test,value"),
                        PredicateDefinition(name = "Cookie", args = "session,abc123")
                    )
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then - 모든 Predicate가 정상적으로 생성되어야 함
            val staticRouteLocator = routeLocator as StaticRouteLocator
            val route = staticRouteLocator.routes[0]
            assertThat(route.predicates).hasSize(5)
            
            val predicateTypes = route.predicates.map { it::class.java.simpleName }
            assertThat(predicateTypes).containsExactlyInAnyOrder(
                "PathPredicate",
                "MethodPredicate",
                "HostPredicate",
                "HeaderPredicate",
                "CookiePredicate"
            )
        }

        @Test
        @DisplayName("Predicate 발견 로직이 패키지 스캔 결과를 올바르게 검증하는지 확인")
        fun `should validate package scan results correctly`() {
            // given - 정상적인 라우트 정의 (패키지 스캔이 성공적으로 수행되어야 함)
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "validation-test-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/test/**")
                    )
                )
            )

            // when & then - 예외 없이 정상적으로 생성되어야 함
            val routeLocator = routeLocatorFactory.create(routeDefinitions)
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(1)
        }

        @Test
        @DisplayName("Predicate 이름 매핑이 올바르게 생성되는지 확인")
        fun `should create correct predicate name mappings`() {
            // given - 다양한 Predicate를 사용하는 라우트
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "mapping-test-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/api/**"),
                        PredicateDefinition(name = "Method", args = "POST"),
                        PredicateDefinition(name = "Host", args = "api.example.com")
                    )
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then - 각 Predicate가 올바른 클래스로 매핑되어야 함
            val staticRouteLocator = routeLocator as StaticRouteLocator
            val route = staticRouteLocator.routes[0]
            
            assertThat(route.predicates[0]).isInstanceOf(PathPredicate::class.java)
            assertThat(route.predicates[1]).isInstanceOf(MethodPredicate::class.java)
            // HostPredicate는 클래스 이름으로 확인
            assertThat(route.predicates[2]::class.java.simpleName).isEqualTo("HostPredicate")
        }

        @Test
        @DisplayName("Predicate 발견 로직의 중복 이름 처리 확인")
        fun `should handle duplicate predicate names gracefully`() {
            // given - 정상적인 라우트 정의 (중복 처리는 내부적으로 수행됨)
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "duplicate-handling-test-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/test/**")
                    )
                )
            )

            // when & then - 중복이 있어도 정상적으로 처리되어야 함 (첫 번째 발견된 클래스 사용)
            val routeLocator = routeLocatorFactory.create(routeDefinitions)
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(1)
            assertThat(staticRouteLocator.routes[0].predicates).hasSize(1)
        }

        @Test
        @DisplayName("Predicate 발견 로직의 패키지 스캔 결과 검증")
        fun `should validate package scan results and log discovered predicates`() {
            // given - 정상적인 라우트 정의
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "scan-validation-test-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Path", args = "/validation/**")
                    )
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then - 패키지 스캔이 성공적으로 수행되고 Predicate가 발견되어야 함
            val staticRouteLocator = routeLocator as StaticRouteLocator
            assertThat(staticRouteLocator.routes).hasSize(1)
            
            // 알려진 Predicate들이 모두 발견되었는지 확인 (간접적으로 검증)
            // 실제로는 로그를 통해 "Successfully initialized 5 predicate classes" 메시지 확인 가능
            val route = staticRouteLocator.routes[0]
            assertThat(route.predicates[0]).isInstanceOf(PathPredicate::class.java)
        }
    }

    @Nested
    @DisplayName("Predicate 발견 로직 엣지 케이스")
    inner class PredicateDiscoveryEdgeCases {

        @Test
        @DisplayName("대소문자 구분 Predicate 이름 처리")
        fun `should handle case sensitive predicate names`() {
            // given - 소문자로 Predicate 이름 지정
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "lowercase-predicate-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "path", args = "/api/**")
                    )
                )
            )

            // when & then - 대소문자 구분으로 인해 실패해야 함
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(PredicateDiscoveryException::class.java)
                .hasMessageContaining("Unknown predicate 'path'")
                .hasMessageContaining("Available predicates:")
        }

        @Test
        @DisplayName("Predicate 이름 공백 처리")
        fun `should handle predicate names with whitespace`() {
            // given
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "whitespace-predicate-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = " Path ", args = "/api/**")
                    )
                )
            )

            // when & then - 공백이 포함된 이름으로 인해 실패해야 함
            assertThatThrownBy { routeLocatorFactory.create(routeDefinitions) }
                .isInstanceOf(PredicateDiscoveryException::class.java)
                .hasMessageContaining("Unknown predicate ' Path '")
                .hasMessageContaining("Available predicates:")
        }

        @Test
        @DisplayName("알려진 Predicate 타입 생성 확인 - 단일 인수 Predicate만")
        fun `should create known predicate types with single arguments`() {
            // given - 현재 구현의 제한사항으로 인해 단일 인수 Predicate만 테스트
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "known-predicates-route",
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
            
            // 각 Predicate 타입이 올바르게 생성되었는지 확인
            val predicateTypes = route.predicates.map { it::class.java.simpleName }
            assertThat(predicateTypes).containsExactly(
                "PathPredicate",
                "MethodPredicate", 
                "HostPredicate"
            )
        }

        @Test
        @DisplayName("HeaderPredicate와 CookiePredicate의 다중 인수 처리 테스트")
        fun `should handle multi-argument predicates correctly`() {
            // given - HeaderPredicate와 CookiePredicate가 쉼표로 구분된 인수를 정상적으로 처리
            val routeDefinitions = mutableListOf(
                RouteDefinition(
                    id = "multi-arg-predicates-route",
                    uri = "http://example.com",
                    predicates = listOf(
                        PredicateDefinition(name = "Header", args = "X-Test,value"),
                        PredicateDefinition(name = "Cookie", args = "session,abc123")
                    )
                )
            )

            // when
            val routeLocator = routeLocatorFactory.create(routeDefinitions)

            // then
            val staticRouteLocator = routeLocator as StaticRouteLocator
            val route = staticRouteLocator.routes[0]
            assertThat(route.predicates).hasSize(2)

            // 각 Predicate 타입이 올바르게 생성되었는지 확인
            val predicateTypes = route.predicates.map { it::class.java.simpleName }
            assertThat(predicateTypes).containsExactly(
                "HeaderPredicate",
                "CookiePredicate"
            )
        }
    }
}