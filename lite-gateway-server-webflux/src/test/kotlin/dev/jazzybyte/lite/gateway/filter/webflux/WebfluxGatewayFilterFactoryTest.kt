package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.exception.FilterInstantiationException
import dev.jazzybyte.lite.gateway.filter.AddRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.AddResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.RemoveRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.RemoveResponseHeaderGatewayFilter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("WebfluxGatewayFilterFactory 테스트")
class WebfluxGatewayFilterFactoryTest {

    private lateinit var factory: WebfluxGatewayFilterFactory

    @BeforeEach
    fun setUp() {
        factory = WebfluxGatewayFilterFactory()
    }

    @Nested
    @DisplayName("AddRequestHeaderGatewayFilter 생성")
    inner class AddRequestHeaderFilterCreation {

        @Test
        @DisplayName("유효한 인자로 필터를 성공적으로 생성한다")
        fun `creates filter successfully with valid arguments`() {
            // Given
            val definition = FilterDefinition(
                type = "AddRequestHeaderGateway",
                args = mapOf("name" to "X-Test-Header", "value" to "test-value")
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(AddRequestHeaderGatewayFilter::class.java)
        }

        @Test
        @DisplayName("name 인자가 누락된 경우 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException when name argument is missing`() {
            // Given
            val definition = FilterDefinition(
                type = "AddRequestHeaderGateway",
                args = mapOf("value" to "test-value")
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Missing required arguments: [name]")
                .hasMessageContaining("AddRequestHeaderGateway")
        }

        @Test
        @DisplayName("value 인자가 누락된 경우 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException when value argument is missing`() {
            // Given
            val definition = FilterDefinition(
                type = "AddRequestHeaderGateway",
                args = mapOf("name" to "X-Test-Header")
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Missing required arguments: [value]")
        }

        @Test
        @DisplayName("빈 문자열 인자가 제공된 경우 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException when blank argument is provided`() {
            // Given
            val definition = FilterDefinition(
                type = "AddRequestHeaderGateway",
                args = mapOf("name" to "", "value" to "test-value")
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Argument 'name' cannot be blank")
        }
    }

    @Nested
    @DisplayName("RemoveRequestHeaderGatewayFilter 생성")
    inner class RemoveRequestHeaderFilterCreation {

        @Test
        @DisplayName("유효한 인자로 필터를 성공적으로 생성한다")
        fun `creates filter successfully with valid arguments`() {
            // Given
            val definition = FilterDefinition(
                type = "RemoveRequestHeaderGateway",
                args = mapOf("name" to "X-Remove-Header")
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(RemoveRequestHeaderGatewayFilter::class.java)
        }

        @Test
        @DisplayName("name 인자가 누락된 경우 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException when name argument is missing`() {
            // Given
            val definition = FilterDefinition(
                type = "RemoveRequestHeaderGateway",
                args = emptyMap()
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Missing required arguments: [name]")
        }
    }

    @Nested
    @DisplayName("AddResponseHeaderGatewayFilter 생성")
    inner class AddResponseHeaderFilterCreation {

        @Test
        @DisplayName("유효한 인자로 필터를 성공적으로 생성한다")
        fun `creates filter successfully with valid arguments`() {
            // Given
            val definition = FilterDefinition(
                type = "AddResponseHeaderGateway",
                args = mapOf("name" to "X-Response-Header", "value" to "response-value")
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(AddResponseHeaderGatewayFilter::class.java)
        }
    }

    @Nested
    @DisplayName("RemoveResponseHeaderGatewayFilter 생성")
    inner class RemoveResponseHeaderFilterCreation {

        @Test
        @DisplayName("유효한 인자로 필터를 성공적으로 생성한다")
        fun `creates filter successfully with valid arguments`() {
            // Given
            val definition = FilterDefinition(
                type = "RemoveResponseHeaderGateway",
                args = mapOf("name" to "X-Remove-Response-Header")
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(RemoveResponseHeaderGatewayFilter::class.java)
        }
    }

    @Nested
    @DisplayName("필터 정의 검증")
    inner class FilterDefinitionValidation {

        @Test
        @DisplayName("빈 필터 타입에 대해 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException for blank filter type`() {
            // Given
            val definition = FilterDefinition(
                type = "",
                args = mapOf("name" to "test")
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Filter type cannot be blank")
        }

        @Test
        @DisplayName("등록되지 않은 필터 타입에 대해 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException for unregistered filter type`() {
            // Given
            val definition = FilterDefinition(
                type = "UnknownFilter",
                args = mapOf("name" to "test")
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Filter type 'UnknownFilter' is not registered")
                .hasMessageContaining("Available filters:")
        }
    }

    @Nested
    @DisplayName("오류 처리")
    inner class ErrorHandling {

        @Test
        @DisplayName("FilterInstantiationException은 적절한 컨텍스트 정보를 포함한다")
        fun `FilterInstantiationException contains proper context information`() {
            // Given
            val definition = FilterDefinition(
                type = "AddRequestHeaderGateway",
                args = mapOf("invalid" to "args")
            )

            // When
            var caughtException: FilterInstantiationException? = null
            try {
                factory.create(definition)
            } catch (e: FilterInstantiationException) {
                caughtException = e
            }

            // Then
            assertThat(caughtException).isNotNull
            assertThat(caughtException!!.message).contains("AddRequestHeaderGateway")
            assertThat(caughtException.args).isEqualTo(definition.args)
        }
    }
}