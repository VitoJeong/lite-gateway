package dev.jazzybyte.lite.gateway.config.validation

import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI

/**
 * URI 형식 검증에 대한 단위 테스트 클래스
 * 
 * 테스트 범위:
 * - UriValidator 단위 테스트
 * - 유효한 URI 형식 검증
 * - 잘못된 URI 형식 처리
 * - 특수한 URI 스키마 처리
 * - URI 파싱 엣지 케이스
 * - 포트 범위 검증
 * - 국제화 도메인 이름 처리
 */
@DisplayName("URI 형식 검증 테스트")
class UriValidatorTest {

    private lateinit var uriValidator: UriValidator

    @BeforeEach
    fun setUp() {
        uriValidator = UriValidator()
    }

    @Nested
    @DisplayName("유효한 URI 검증")
    inner class ValidUriTest {

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
        fun `should pass validation for basic HTTP and HTTPS URIs`(uri: String) {
            // when & then - 예외가 발생하지 않아야 함
            uriValidator.validateUriFormat(uri, "test-route")
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
        fun `should pass validation for URIs with path, query, and fragment`(uri: String) {
            // when & then - 예외가 발생하지 않아야 함
            uriValidator.validateUriFormat(uri, "test-route")
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
        fun `should pass validation for URIs with various schemes`(uri: String) {
            // when & then - 예외가 발생하지 않아야 함
            uriValidator.validateUriFormat(uri, "test-route")
        }

        @Test
        @DisplayName("국제화 도메인 이름(IDN)은 검증을 통과해야 함")
        fun `should pass validation for internationalized domain names`() {
            // given
            val uri = "http://한글도메인.com"
            
            // when & then - 예외가 발생하지 않아야 함
            uriValidator.validateUriFormat(uri, "test-route")
        }

        @Test
        @DisplayName("특수 문자가 URL 인코딩된 URI는 유효해야 함")
        fun `should pass validation for URIs with URL-encoded special characters`() {
            // given
            val uri = "http://example.com/path%20with%20spaces?param=value%20with%20spaces"
            
            // when & then - 예외가 발생하지 않아야 함
            uriValidator.validateUriFormat(uri, "test-route")
        }

        @Test
        @DisplayName("매우 긴 URI는 처리되어야 함")
        fun `should handle very long URIs`() {
            // given - 매우 긴 경로를 가진 URI
            val longPath = "/api/" + "a".repeat(1000)
            val uri = "http://example.com$longPath"
            
            // when & then - 예외 없이 처리되어야 함
            uriValidator.validateUriFormat(uri, "test-route")
        }
    }

    @Nested
    @DisplayName("잘못된 URI 검증")
    inner class InvalidUriTest {

        @ParameterizedTest
        @ValueSource(strings = [
            "invalid-uri",
            "://example.com",
            "http:",
            "http://",
            ""
        ])
        @DisplayName("유효하지 않은 URI는 검증 실패해야 함")
        fun `should fail validation for invalid URIs`(uri: String) {
            // when & then
            val exception = assertThrows<RouteConfigurationException> {
                uriValidator.validateUriFormat(uri, "test-route")
            }
            
            assertThat(exception.routeId).isEqualTo("test-route")
            assertThat(exception.message).contains("Invalid URI format")
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "not-a-uri",
            "://missing-scheme",
            "http:///path-without-host",
            "http://host with spaces",
            "http://host:invalid-port",
            "http://host:-1",
            "http://host:99999"
        ])
        @DisplayName("잘못된 URI 형식은 예외를 발생시켜야 함")
        fun `should throw exception for malformed URI formats`(invalidUri: String) {
            // when & then
            assertThatThrownBy { 
                uriValidator.validateUriFormat(invalidUri, "test-route")
            }.isInstanceOf(RouteConfigurationException::class.java)
        }

        @Test
        @DisplayName("포트 범위를 벗어난 URI는 검증 실패해야 함")
        fun `should fail validation for URI with invalid port range`() {
            // given
            val uri = "http://example.com:70000"
            
            // when & then
            val exception = assertThrows<RouteConfigurationException> {
                uriValidator.validateUriFormat(uri, "test-route")
            }
            
            assertThat(exception.message).contains("Port must be between 1 and 65535")
        }

        @Test
        @DisplayName("인코딩되지 않은 공백이 있는 URI는 검증 실패해야 함")
        fun `should fail validation for URI with unencoded spaces`() {
            // given
            val uri = "http://example.com/path with spaces"
            
            // when & then
            val exception = assertThrows<RouteConfigurationException> {
                uriValidator.validateUriFormat(uri, "test-route")
            }
            
            assertThat(exception.message).contains("Illegal character in path")
        }

        @Test
        @DisplayName("null URI는 예외를 발생시켜야 함")
        fun `should throw exception for null URI`() {
            // when & then - 컴파일 타임에 null을 전달할 수 없으므로 URI 생성자를 직접 테스트
            assertThatThrownBy {
                URI(null)
            }.isInstanceOf(NullPointerException::class.java)
        }
    }

    @Nested
    @DisplayName("엣지 케이스 검증")
    inner class EdgeCaseTest {

        @Test
        @DisplayName("빈 문자열 URI는 검증 실패해야 함")
        fun `should fail validation for empty URI`() {
            // when & then
            val exception = assertThrows<RouteConfigurationException> {
                uriValidator.validateUriFormat("", "test-route")
            }
            
            assertThat(exception.routeId).isEqualTo("test-route")
            assertThat(exception.message).contains("Invalid URI format")
        }

        @Test
        @DisplayName("공백만 있는 URI는 검증 실패해야 함")
        fun `should fail validation for whitespace-only URI`() {
            // when & then
            val exception = assertThrows<RouteConfigurationException> {
                uriValidator.validateUriFormat("   ", "test-route")
            }
            
            assertThat(exception.routeId).isEqualTo("test-route")
            assertThat(exception.message).contains("Invalid URI format")
        }

        @Test
        @DisplayName("유니코드 문자가 포함된 URI는 처리되어야 함")
        fun `should handle URIs with unicode characters`() {
            // given
            val uri = "http://example.com/경로/한글"
            
            // when & then - 예외가 발생하지 않아야 함 (URL 인코딩이 필요하지만 URI 자체는 유효)
            uriValidator.validateUriFormat(uri, "test-route")
        }
    }
}