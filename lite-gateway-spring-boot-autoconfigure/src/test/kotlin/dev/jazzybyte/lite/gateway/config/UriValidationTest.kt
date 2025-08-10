package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import dev.jazzybyte.lite.gateway.route.PredicateDefinition
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI

/**
 * URI 형식 검증에 대한 추가 테스트 케이스
 * 
 * 테스트 범위:
 * - 유효한 URI 형식 검증
 * - 잘못된 URI 형식 처리
 * - 특수한 URI 스키마 처리
 * - URI 파싱 엣지 케이스
 */
@DisplayName("URI 형식 검증 테스트")
class UriValidationTest {

    @Nested
    @DisplayName("유효한 URI 형식 테스트")
    inner class ValidUriFormatTest {

        @ParameterizedTest
        @ValueSource(strings = [
            "http://example.com",
            "https://example.com",
            "http://example.com:8080",
            "https://example.com:8443",
            "http://localhost",
            "http://localhost:3000",
            "http://127.0.0.1",
            "http://127.0.0.1:8080",
            "http://[::1]",
            "http://[::1]:8080"
        ])
        @DisplayName("기본 HTTP/HTTPS URI는 유효해야 함")
        fun `should accept basic HTTP and HTTPS URIs`(uri: String) {
            // given
            val routeDefinition = RouteDefinition(
                id = "valid-uri-route",
                uri = uri,
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when & then - RouteLocatorFactory를 통해 Route 생성이 성공해야 함
            val routeLocator = RouteLocatorFactory.create(mutableListOf(routeDefinition))
            assertThat(routeLocator).isNotNull
        }
        @ParameterizedTest
        @ValueSource(strings = [
            "http://example.com/path",
            "http://example.com/path/to/resource",
            "http://example.com/path?query=value",
            "http://example.com/path?query=value&other=test",
            "http://example.com/path#fragment",
            "http://example.com/path?query=value#fragment"
        ])
        @DisplayName("경로, 쿼리, 프래그먼트가 포함된 URI는 유효해야 함")
        fun `should accept URIs with path, query, and fragment`(uri: String) {
            // given
            val routeDefinition = RouteDefinition(
                id = "complex-uri-route",
                uri = uri,
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when & then
            val routeLocator = RouteLocatorFactory.create(mutableListOf(routeDefinition))
            assertThat(routeLocator).isNotNull
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "ws://example.com",
            "wss://example.com",
            "ftp://example.com",
            "file:///path/to/file",
            "custom://example.com"
        ])
        @DisplayName("다양한 스키마의 URI는 유효해야 함")
        fun `should accept URIs with various schemes`(uri: String) {
            // given
            val routeDefinition = RouteDefinition(
                id = "scheme-uri-route",
                uri = uri,
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when & then
            val routeLocator = RouteLocatorFactory.create(mutableListOf(routeDefinition))
            assertThat(routeLocator).isNotNull
        }

        @Test
        @DisplayName("국제화 도메인 이름(IDN)은 유효해야 함")
        fun `should accept internationalized domain names`() {
            // given
            val routeDefinition = RouteDefinition(
                id = "idn-uri-route",
                uri = "http://한국.com",
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when & then
            val routeLocator = RouteLocatorFactory.create(mutableListOf(routeDefinition))
            assertThat(routeLocator).isNotNull
        }

        @Test
        @DisplayName("특수 문자가 URL 인코딩된 URI는 유효해야 함")
        fun `should accept URIs with URL-encoded special characters`() {
            // given
            val routeDefinition = RouteDefinition(
                id = "encoded-uri-route",
                uri = "http://example.com/path%20with%20spaces?param=value%20with%20spaces",
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when & then
            val routeLocator = RouteLocatorFactory.create(mutableListOf(routeDefinition))
            assertThat(routeLocator).isNotNull
        }
    }

    @Nested
    @DisplayName("잘못된 URI 형식 테스트")
    inner class InvalidUriFormatTest {

        @ParameterizedTest
        @ValueSource(strings = [
            "not-a-uri",
            "://missing-scheme",
            "http://",
            "http:///path-without-host",
            "http://host with spaces",
            "http://host:invalid-port",
            "http://host:-1",
            "http://host:99999"
        ])
        @DisplayName("잘못된 URI 형식은 예외를 발생시켜야 함")
        fun `should throw exception for invalid URI formats`(invalidUri: String) {
            // given
            val routeDefinition = RouteDefinition(
                id = "invalid-uri-route",
                uri = invalidUri,
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when & then
            assertThatThrownBy { 
                RouteLocatorFactory.create(mutableListOf(routeDefinition))
            }.isInstanceOf(RouteConfigurationException::class.java)
        }

        @Test
        @DisplayName("null URI는 예외를 발생시켜야 함")
        fun `should throw exception for null URI`() {
            // when & then - 컴파일 타임에 null을 전달할 수 없으므로 URI 생성자를 직접 테스트
            assertThatThrownBy {
                URI(null)
            }.isInstanceOf(NullPointerException::class.java)
        }

        @Test
        @DisplayName("매우 긴 URI는 처리되어야 함")
        fun `should handle very long URIs`() {
            // given - 매우 긴 경로를 가진 URI
            val longPath = "/api/" + "a".repeat(1000)
            val routeDefinition = RouteDefinition(
                id = "long-uri-route",
                uri = "http://example.com$longPath",
                predicates = listOf(
                    PredicateDefinition(name = "Path", args = "/api/**")
                )
            )

            // when & then - 예외 없이 처리되어야 함
            val routeLocator = RouteLocatorFactory.create(mutableListOf(routeDefinition))
            assertThat(routeLocator).isNotNull
        }
    }
}