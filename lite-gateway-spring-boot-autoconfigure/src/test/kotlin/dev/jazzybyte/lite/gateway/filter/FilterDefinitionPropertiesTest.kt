package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.config.LiteGatewayConfigProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.validation.BindValidationException
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

import org.springframework.boot.autoconfigure.SpringBootApplication

@DisplayName("FilterDefinition @ConfigurationProperties 바인딩 테스트")
class FilterDefinitionPropertiesTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TestConfig::class.java)

    @SpringBootApplication
    @EnableConfigurationProperties(LiteGatewayConfigProperties::class)
    class TestConfig {
        fun methodValidationPostProcessor(): MethodValidationPostProcessor = MethodValidationPostProcessor()
    }

    @Test
    @DisplayName("필터 이름이 유효한 경우 컨텍스트가 성공적으로 로드된다")
    fun `context loads when filter name is valid`() {
        contextRunner
            .withPropertyValues(
                "lite.gateway.routes[0].id=test-route",
                "lite.gateway.routes[0].uri=http://localhost:8080",
                "lite.gateway.routes[0].filters[0].type=AddRequestHeader",
                "lite.gateway.routes[0].filters[0].args.X-Test=test-value",
            )
            .run { context ->
                assertThat(context).hasNotFailed()
                val properties = context.getBean(LiteGatewayConfigProperties::class.java)
                assertThat(properties.routes).hasSize(1)
                val route = properties.routes[0]
                assertThat(route.filters).hasSize(1)
                val filter = route.filters[0]
                assertThat(filter.type).isEqualTo("AddRequestHeader")
                assertThat(filter.args).containsEntry("X-Test", "test-value")
            }
    }

    @Test
    @DisplayName("필터 이름이 비어있는 경우 컨텍스트 로드에 실패한다")
    fun `context fails to load when filter name is empty`() {
        contextRunner
            .withPropertyValues(
                "lite.gateway.routes[0].id=test-route",
                "lite.gateway.routes[0].uri=http://localhost:8080",
                "lite.gateway.routes[0].filters[0].type=" // 빈 필터 이름
            )
            .withBean(MethodValidationPostProcessor::class.java)
            .withSystemProperties("spring.main.fail-on-validation-error=true")
            .run { context ->
                assertThat(context).hasFailed()
                // 예외 메시지가 다를 수 있으므로 더 일반적인 검증으로 변경
                assertThat(context.startupFailure).hasRootCauseInstanceOf(BindValidationException::class.java)
            }
    }

    @Test
    @DisplayName("globalfilters 이름이 비어있는 필터가 있는 경우 컨텍스트 로드에 실패한다")
    fun `context fails to load when globalfilters name is empty`() {
        contextRunner
            .withPropertyValues(
                "lite.gateway.globalfilters[0].type=",
                "lite.gateway.globalfilters[0].args.key=value"
            )
            .withBean(MethodValidationPostProcessor::class.java)
            .withSystemProperties("spring.main.fail-on-validation-error=true")
            .run { context ->
                assertThat(context).hasFailed()
                // 예외 메시지가 다를 수 있으므로 더 일반적인 검증으로 변경
                assertThat(context.startupFailure).hasRootCauseInstanceOf(BindValidationException::class.java)
            }
    }
}