package dev.jazzybyte.lite.gateway.exception

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@DisplayName("FilterConfigurationException 테스트")
class FilterConfigurationExceptionTest {

    @Test
    @DisplayName("필터 타입만으로 예외를 생성할 수 있다")
    fun `should create exception with filter type only`() {
        // given
        val message = "Invalid configuration"
        val filterType = "AddRequestHeaderFilter"
        
        // when
        val exception = FilterConfigurationException(message, filterType)
        
        // then
        assertEquals("Filter configuration error (filter: 'AddRequestHeaderFilter'): Invalid configuration", exception.message)
        assertEquals(filterType, exception.filterType)
        assertNull(exception.configKey)
        assertNull(exception.configValue)
        assertNull(exception.cause)
    }

    @Test
    @DisplayName("모든 설정 정보와 함께 예외를 생성할 수 있다")
    fun `should create exception with all configuration information`() {
        // given
        val message = "Invalid header name format"
        val filterType = "AddRequestHeaderFilter"
        val configKey = "headerName"
        val configValue = "invalid-header-name!"
        val cause = IllegalArgumentException("Special characters not allowed")
        
        // when
        val exception = FilterConfigurationException(message, filterType, configKey, configValue, cause)
        
        // then
        assertEquals(
            "Filter configuration error (filter: 'AddRequestHeaderFilter', config: headerName='invalid-header-name!'): Invalid header name format",
            exception.message
        )
        assertEquals(filterType, exception.filterType)
        assertEquals(configKey, exception.configKey)
        assertEquals(configValue, exception.configValue)
        assertSame(cause, exception.cause)
    }

    @Test
    @DisplayName("설정 키만 있는 경우 적절한 메시지를 생성한다")
    fun `should create appropriate message with config key only`() {
        // given
        val message = "Missing required configuration"
        val filterType = "CircuitBreakerFilter"
        val configKey = "failureRateThreshold"
        
        // when
        val exception = FilterConfigurationException(message, filterType, configKey)
        
        // then
        assertEquals(
            "Filter configuration error (filter: 'CircuitBreakerFilter', config: failureRateThreshold): Missing required configuration",
            exception.message
        )
        assertEquals(filterType, exception.filterType)
        assertEquals(configKey, exception.configKey)
        assertNull(exception.configValue)
    }

    @Test
    @DisplayName("설정 값이 null인 경우에도 키를 표시한다")
    fun `should display key even when config value is null`() {
        // given
        val message = "Configuration value cannot be null"
        val filterType = "RateLimitFilter"
        val configKey = "replenishRate"
        val configValue: String? = null
        
        // when
        val exception = FilterConfigurationException(message, filterType, configKey, configValue)
        
        // then
        assertEquals(
            "Filter configuration error (filter: 'RateLimitFilter', config: replenishRate): Configuration value cannot be null",
            exception.message
        )
        assertEquals(filterType, exception.filterType)
        assertEquals(configKey, exception.configKey)
        assertNull(exception.configValue)
    }
}