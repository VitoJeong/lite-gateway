package dev.jazzybyte.lite.gateway.filter

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("CriticalFilter 인터페이스 테스트")
class CriticalFilterTest {

    @Test
    @DisplayName("기본 실패 상태 코드는 500이다")
    fun `should return default failure status code 500`() {
        // given
        val criticalFilter = object : CriticalFilter {}

        // when
        val statusCode = criticalFilter.getFailureStatusCode()

        // then
        assertEquals(500, statusCode)
    }

    @Test
    @DisplayName("기본 실패 메시지를 반환한다")
    fun `should return default failure message`() {
        // given
        val criticalFilter = object : CriticalFilter {}
        val cause = RuntimeException("Test exception")

        // when
        val message = criticalFilter.getFailureMessage(cause)

        // then
        assertEquals("Filter execution failed", message)
    }

    @Test
    @DisplayName("커스텀 실패 상태 코드를 설정할 수 있다")
    fun `should allow custom failure status code`() {
        // given
        val criticalFilter = object : CriticalFilter {
            override fun getFailureStatusCode(): Int = 401
        }

        // when
        val statusCode = criticalFilter.getFailureStatusCode()

        // then
        assertEquals(401, statusCode)
    }

    @Test
    @DisplayName("커스텀 실패 메시지를 설정할 수 있다")
    fun `should allow custom failure message`() {
        // given
        val criticalFilter = object : CriticalFilter {
            override fun getFailureMessage(cause: Throwable): String = "Authentication failed: ${cause.message}"
        }
        val cause = RuntimeException("Invalid token")

        // when
        val message = criticalFilter.getFailureMessage(cause)

        // then
        assertEquals("Authentication failed: Invalid token", message)
    }
}