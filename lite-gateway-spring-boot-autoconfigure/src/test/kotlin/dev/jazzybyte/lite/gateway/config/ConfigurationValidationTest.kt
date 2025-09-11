package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.client.HttpClientProperties
import dev.jazzybyte.lite.gateway.route.PredicateDefinition
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * 설정 검증 강화를 위한 전용 테스트 클래스
 * 
 * 테스트 범위:
 * - RouteDefinition Bean Validation 테스트
 * - PredicateDefinition 인수 파싱 검증
 * - URI 형식 검증 테스트
 * - LiteGatewayConfigProperties 검증 테스트
 */
@DisplayName("설정 검증 강화 테스트")
class ConfigurationValidationTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Nested
    @DisplayName("RouteDefinition 검증 로직 확장 테스트")
    inner class RouteDefinitionValidationTest {

        @Test
        @DisplayName("유효한 RouteDefinition은 검증을 통과해야 함")
        fun `should pass validation for valid RouteDefinition`() {
            // given
            val routeDefinition = RouteDefinition(
                id = "valid-route",
                uri = "http://example.com",
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                ),
                order = 1
            )

            // when
            val violations = validator.validate(routeDefinition)

            // then
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("빈 URI는 검증 실패해야 함")
        fun `should fail validation for empty URI`() {
            // given
            val routeDefinition = RouteDefinition(
                id = "empty-uri-route",
                uri = "",
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when
            val violations = validator.validate(routeDefinition)

            // then
            assertThat(violations).hasSize(1)
            val violation = violations.first()
            assertThat(violation.propertyPath.toString()).isEqualTo("uri")
            assertThat(violation.message).isEqualTo("URI는 비어 있을 수 없습니다.")
        }

        @Test
        @DisplayName("공백만 있는 URI는 검증 실패해야 함")
        fun `should fail validation for whitespace-only URI`() {
            // given
            val routeDefinition = RouteDefinition(
                id = "whitespace-uri-route",
                uri = "   ",
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when
            val violations = validator.validate(routeDefinition)

            // then
            assertThat(violations).hasSize(1)
            val violation = violations.first()
            assertThat(violation.propertyPath.toString()).isEqualTo("uri")
            assertThat(violation.message).isEqualTo("URI는 비어 있을 수 없습니다.")
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, -10, -100])
        @DisplayName("음수 order 값은 검증 실패해야 함")
        fun `should fail validation for negative order values`(order: Int) {
            // given
            val routeDefinition = RouteDefinition(
                id = "negative-order-route",
                uri = "http://example.com",
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                ),
                order = order
            )

            // when
            val violations = validator.validate(routeDefinition)

            // then
            assertThat(violations).hasSize(1)
            val violation = violations.first()
            assertThat(violation.propertyPath.toString()).isEqualTo("order")
            assertThat(violation.message).isEqualTo("order는 0 이상이어야 합니다.")
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 10, 100, Int.MAX_VALUE])
        @DisplayName("유효한 order 값은 검증을 통과해야 함")
        fun `should pass validation for valid order values`(order: Int) {
            // given
            val routeDefinition = RouteDefinition(
                id = "valid-order-route",
                uri = "http://example.com",
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                ),
                order = order
            )

            // when
            val violations = validator.validate(routeDefinition)

            // then
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("중첩된 PredicateDefinition 검증 오류도 포함되어야 함")
        fun `should include nested PredicateDefinition validation errors`() {
            // given
            val routeDefinition = RouteDefinition(
                id = "nested-validation-route",
                uri = "http://example.com",
                predicates = listOf(
                    PredicateDefinition(name = "", args = "/api/**") // 빈 name
                )
            )

            // when
            val violations = validator.validate(routeDefinition)

            // then
            assertThat(violations).hasSize(1)
            val violation = violations.first()
            assertThat(violation.propertyPath.toString()).isEqualTo("predicates[0].name")
            assertThat(violation.message).isEqualTo("비어 있을 수 없습니다")
        }

        @Test
        @DisplayName("다중 검증 오류가 모두 포함되어야 함")
        fun `should include multiple validation errors`() {
            // given
            val routeDefinition = RouteDefinition(
                id = "multiple-errors-route",
                uri = "", // 빈 URI
                predicates = listOf(
                    PredicateDefinition(name = "", args = "/api/**") // 빈 name
                ),
                order = -1 // 음수 order
            )

            // when
            val violations = validator.validate(routeDefinition)

            // then
            assertThat(violations).hasSize(3)
            val violationPaths = violations.map { it.propertyPath.toString() }
            assertThat(violationPaths).containsExactlyInAnyOrder(
                "uri",
                "predicates[0].name", 
                "order"
            )
        }
    }

    @Nested
    @DisplayName("PredicateDefinition 인수 파싱 검증 강화")
    inner class PredicateDefinitionArgumentParsingTest {

        @Test
        @DisplayName("유효한 PredicateDefinition은 검증을 통과해야 함")
        fun `should pass validation for valid PredicateDefinition`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Path",
                args = "/api/**"
            )

            // when
            val violations = validator.validate(predicateDefinition)

            // then
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("빈 name은 검증 실패해야 함")
        fun `should fail validation for empty name`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "",
                args = "/api/**"
            )

            // when
            val violations = validator.validate(predicateDefinition)

            // then
            assertThat(violations).hasSize(1)
            val violation = violations.first()
            assertThat(violation.propertyPath.toString()).isEqualTo("name")
            assertThat(violation.message).isEqualTo("비어 있을 수 없습니다")
        }

        @Test
        @DisplayName("공백만 있는 name은 검증 실패해야 함")
        fun `should fail validation for whitespace-only name`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "   ",
                args = "/api/**"
            )

            // when
            val violations = validator.validate(predicateDefinition)

            // then
            assertThat(violations).hasSize(1)
            val violation = violations.first()
            assertThat(violation.propertyPath.toString()).isEqualTo("name")
            assertThat(violation.message).isEqualTo("비어 있을 수 없습니다")
        }

        @Test
        @DisplayName("null args는 빈 배열로 파싱되어야 함")
        fun `should parse null args as empty array`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Path",
                args = null
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).isEmpty()
        }

        @Test
        @DisplayName("빈 args는 빈 배열로 파싱되어야 함")
        fun `should parse empty args as empty array`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Path",
                args = ""
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).isEmpty()
        }

        @Test
        @DisplayName("공백만 있는 args는 빈 배열로 파싱되어야 함")
        fun `should parse whitespace-only args as empty array`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Path",
                args = "   "
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).isEmpty()
        }

        @Test
        @DisplayName("단일 인수는 올바르게 파싱되어야 함")
        fun `should parse single argument correctly`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Path",
                args = "/api/**"
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).containsExactly("/api/**")
        }

        @Test
        @DisplayName("쉼표로 구분된 다중 인수는 올바르게 파싱되어야 함")
        fun `should parse comma-separated multiple arguments correctly`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Cookie",
                args = "session,abc123"
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).containsExactly("session", "abc123")
        }

        @Test
        @DisplayName("공백이 포함된 인수는 트림되어 파싱되어야 함")
        fun `should parse arguments with whitespace by trimming`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Cookie",
                args = " session , abc123 , def456 "
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).containsExactly("session", "abc123", "def456")
        }

        @Test
        @DisplayName("빈 인수는 필터링되어야 함")
        fun `should filter out empty arguments`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Test",
                args = "arg1,,arg2,   ,arg3"
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).containsExactly("arg1", "arg2", "arg3")
        }

        @Test
        @DisplayName("특수 문자가 포함된 인수는 올바르게 파싱되어야 함")
        fun `should parse arguments with special characters correctly`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Header",
                args = "X-Custom-Header,value-with-special-chars!@#$%"
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).containsExactly("X-Custom-Header", "value-with-special-chars!@#$%")
        }

        @Test
        @DisplayName("URL 인코딩된 인수는 그대로 파싱되어야 함")
        fun `should parse URL-encoded arguments as-is`() {
            // given
            val predicateDefinition = PredicateDefinition(
                name = "Query",
                args = "param,value%20with%20spaces"
            )

            // when
            val parsedArgs = predicateDefinition.parsedArgs

            // then
            assertThat(parsedArgs).containsExactly("param", "value%20with%20spaces")
        }
    }

    @Nested
    @DisplayName("LiteGatewayConfigProperties 검증 테스트")
    inner class LiteGatewayConfigPropertiesValidationTest {

        @Test
        @DisplayName("유효한 설정은 검증을 통과해야 함")
        fun `should pass validation for valid configuration`() {
            // given
            val properties = LiteGatewayConfigProperties(
                routes = mutableListOf(
                    RouteDefinition(
                        id = "valid-route",
                        uri = "http://example.com",
                        predicates = listOf(
                            PredicateDefinition(name = "Path", args = "/api/**")
                        )
                    )
                )
            )

            // when
            val violations = validator.validate(properties)

            // then
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("빈 routes 목록은 검증을 통과해야 함")
        fun `should pass validation for empty routes list`() {
            // given
            val properties = LiteGatewayConfigProperties(
                routes = mutableListOf()
            )

            // when
            val violations = validator.validate(properties)

            // then
            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("중첩된 RouteDefinition 검증 오류가 포함되어야 함")
        fun `should include nested RouteDefinition validation errors`() {
            // given
            val properties = LiteGatewayConfigProperties(
                routes = mutableListOf(
                    RouteDefinition(
                        id = "invalid-route",
                        uri = "", // 빈 URI
                        predicates = listOf(
                            PredicateDefinition(name = "", args = "/api/**") // 빈 name
                        ),
                        order = -1 // 음수 order
                    )
                )
            )

            // when
            val violations = validator.validate(properties)

            // then
            assertThat(violations).hasSize(3)
            val violationPaths = violations.map { it.propertyPath.toString() }
            assertThat(violationPaths).containsExactlyInAnyOrder(
                "routes[0].uri",
                "routes[0].predicates[0].name",
                "routes[0].order"
            )
        }

        @Test
        @DisplayName("다중 라우트의 검증 오류가 모두 포함되어야 함")
        fun `should include validation errors from multiple routes`() {
            // given
            val properties = LiteGatewayConfigProperties(
                routes = mutableListOf(
                    RouteDefinition(
                        id = "invalid-route-1",
                        uri = "", // 빈 URI
                        predicates = listOf(
                            PredicateDefinition(name = "Path", args = "/api/**")
                        )
                    ),
                    RouteDefinition(
                        id = "invalid-route-2",
                        uri = "http://example.com",
                        predicates = listOf(
                            PredicateDefinition(name = "", args = "/api/**") // 빈 name
                        ),
                        order = -1 // 음수 order
                    )
                )
            )

            // when
            val violations = validator.validate(properties)

            // then
            assertThat(violations).hasSize(3)
            val violationPaths = violations.map { it.propertyPath.toString() }
            assertThat(violationPaths).containsExactlyInAnyOrder(
                "routes[0].uri",
                "routes[1].predicates[0].name",
                "routes[1].order"
            )
        }

        @Test
        @DisplayName("유효하지 않은 HttpClient 설정은 검증에 실패해야 함")
        fun `should fail validation for invalid http client properties`() {
            // given
            val properties = LiteGatewayConfigProperties(
                httpClient = HttpClientProperties(
                    maxConnections = 0,
                    connectionTimeout = -1,
                    maxHeaderSize = 0,
                    acquireTimeout = -1
                )
            )

            // when
            val violations = validator.validate(properties)

            // then
            assertThat(violations).hasSize(4)
            val violationMessages = violations.associate { it.propertyPath.toString() to it.message }
            assertThat(violationMessages).containsEntry("httpClient.maxConnections", "maxConnections는 1 이상이어야 합니다.")
            assertThat(violationMessages).containsEntry("httpClient.connectionTimeout", "connectionTimeout은 0 이상이어야 합니다.")
            assertThat(violationMessages).containsEntry("httpClient.maxHeaderSize", "maxHeaderSize는 1 이상이어야 합니다.")
            assertThat(violationMessages).containsEntry("httpClient.acquireTimeout", "acquireTimeout은 0 이상이어야 합니다.")
        }

        @Test
        @DisplayName("유효한 HttpClient 설정은 검증을 통과해야 함")
        fun `should pass validation for valid http client properties`() {
            // given
            val properties = LiteGatewayConfigProperties(
                httpClient = HttpClientProperties(
                    maxConnections = 1,
                    connectionTimeout = 0,
                    maxHeaderSize = 1,
                    acquireTimeout = 0
                )
            )

            // when
            val violations = validator.validate(properties)

            // then
            assertThat(violations).isEmpty()
            val violationPaths = violations.map { it.propertyPath.toString() }
            assertThat(violationPaths).doesNotContain("httpClient.maxConnections")
            assertThat(violationPaths).doesNotContain("httpClient.connectionTimeout")
            assertThat(violationPaths).doesNotContain("httpClient.maxHeaderSize")
            assertThat(violationPaths).doesNotContain("httpClient.acquireTimeout")
        }
    }
}