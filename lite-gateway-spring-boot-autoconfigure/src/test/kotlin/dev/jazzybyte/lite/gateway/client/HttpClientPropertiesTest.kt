package dev.jazzybyte.lite.gateway.client

import dev.jazzybyte.lite.gateway.config.LiteGatewayConfigProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.validation.BindValidationException
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

@DisplayName("HttpClientProperties @ConfigurationProperties 바인딩 테스트")
class HttpClientPropertiesTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java)

    @SpringBootApplication
    @EnableConfigurationProperties(LiteGatewayConfigProperties::class)
    class TestConfig {
        fun methodValidationPostProcessor(): MethodValidationPostProcessor = MethodValidationPostProcessor()
    }

    @Test
    @DisplayName("HttpClient 속성이 유효한 경우 컨텍스트가 성공적으로 로드된다")
    fun `context loads when http client properties are valid`() {
        contextRunner
            .withPropertyValues(
                "lite.gateway.http-client.max-connections=100",
                "lite.gateway.http-client.connection-timeout=1000",
                "lite.gateway.http-client.max-header-size=4096",
                "lite.gateway.http-client.acquire-timeout=2000"
            )
            .run { context ->
                assertThat(context).hasNotFailed()
                val properties = context.getBean(LiteGatewayConfigProperties::class.java)
                val httpClient = properties.httpClient
                assertThat(httpClient.maxConnections).isEqualTo(100)
                assertThat(httpClient.connectionTimeout).isEqualTo(1000)
                assertThat(httpClient.maxHeaderSize).isEqualTo(4096)
                assertThat(httpClient.acquireTimeout).isEqualTo(2000)
            }
    }

    @Test
    @DisplayName("기본 HttpClient 속성으로 컨텍스트가 성공적으로 로드된다")
    fun `context loads with default http client properties`() {
        contextRunner
            .run { context ->
                assertThat(context).hasNotFailed()
                val properties = context.getBean(LiteGatewayConfigProperties::class.java)
                val httpClient = properties.httpClient
                assertThat(httpClient.maxConnections).isEqualTo(500)
                assertThat(httpClient.connectionTimeout).isEqualTo(5000)
                assertThat(httpClient.maxHeaderSize).isEqualTo(8192)
                assertThat(httpClient.acquireTimeout).isEqualTo(10000)
            }
    }

    @Test
    @DisplayName("maxConnections가 0이면 컨텍스트 로드에 실패한다")
    fun `context fails to load when maxConnections is zero`() {
        contextRunner
            .withPropertyValues("lite.gateway.http-client.max-connections=0")
            .withBean(MethodValidationPostProcessor::class.java)
            .withSystemProperties("spring.main.fail-on-validation-error=true")
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure).hasRootCauseInstanceOf(BindValidationException::class.java)
            }
    }

    @Test
    @DisplayName("connectionTimeout이 음수이면 컨텍스트 로드에 실패한다")
    fun `context fails to load when connectionTimeout is negative`() {
        contextRunner
            .withPropertyValues("lite.gateway.http-client.connection-timeout=-1")
            .withBean(MethodValidationPostProcessor::class.java)
            .withSystemProperties("spring.main.fail-on-validation-error=true")
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure).hasRootCauseInstanceOf(BindValidationException::class.java)
            }
    }

    @Test
    @DisplayName("maxHeaderSize가 0이면 컨텍스트 로드에 실패한다")
    fun `context fails to load when maxHeaderSize is zero`() {
        contextRunner
            .withPropertyValues("lite.gateway.http-client.max-header-size=0")
            .withBean(MethodValidationPostProcessor::class.java)
            .withSystemProperties("spring.main.fail-on-validation-error=true")
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure).hasRootCauseInstanceOf(BindValidationException::class.java)
            }
    }

    @Test
    @DisplayName("acquireTimeout이 음수이면 컨텍스트 로드에 실패한다")
    fun `context fails to load when acquireTimeout is negative`() {
        contextRunner
            .withPropertyValues("lite.gateway.http-client.acquire-timeout=-1")
            .withBean(MethodValidationPostProcessor::class.java)
            .withSystemProperties("spring.main.fail-on-validation-error=true")
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure).hasRootCauseInstanceOf(BindValidationException::class.java)
            }
    }
}
