package dev.jazzybyte.lite.gateway.config.validation

import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("UriValidator 테스트")
class UriValidatorTest {

    private lateinit var uriValidator: UriValidator

    @BeforeEach
    fun setUp() {
        uriValidator = UriValidator()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "http://example.com",
        "https://example.com:8080",
        "http://localhost:3000/api",
        "https://api.example.com/v1/users"
    ])
    @DisplayName("유효한 URI는 검증을 통과해야 함")
    fun `should pass validation for valid URIs`(uri: String) {
        // when & then - 예외가 발생하지 않아야 함
        uriValidator.validateUriFormat(uri, "test-route")
    }

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
    @DisplayName("국제화 도메인 이름(IDN)은 검증을 통과해야 함")
    fun `should pass validation for internationalized domain names`() {
        // given
        val uri = "http://한글도메인.com"
        
        // when & then - 예외가 발생하지 않아야 함
        uriValidator.validateUriFormat(uri, "test-route")
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
}