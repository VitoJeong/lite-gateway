package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.exception.FilterInstantiationException
import dev.jazzybyte.lite.gateway.filter.AddRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.AddResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.ModifyRequestBodyGatewayFilter
import dev.jazzybyte.lite.gateway.filter.ModifyResponseBodyGatewayFilter
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
                type = "AddRequestHeader",
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
                type = "AddRequestHeader",
                args = mapOf("value" to "test-value")
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Missing required arguments: [name]")
                .hasMessageContaining("AddRequestHeader")
        }

        @Test
        @DisplayName("value 인자가 누락된 경우 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException when value argument is missing`() {
            // Given
            val definition = FilterDefinition(
                type = "AddRequestHeader",
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
                type = "AddRequestHeader",
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
                type = "RemoveRequestHeader",
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
                type = "RemoveRequestHeader",
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
                type = "AddResponseHeader",
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
                type = "RemoveResponseHeader",
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
    @DisplayName("ModifyRequestBodyGatewayFilter 생성")
    inner class ModifyRequestBodyFilterCreation {

        @Test
        @DisplayName("대문자 변환 필터를 성공적으로 생성한다")
        fun `creates uppercase transform filter successfully`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyRequestBody",
                args = mapOf("transformType" to "uppercase")
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(ModifyRequestBodyGatewayFilter::class.java)
        }

        @Test
        @DisplayName("마스킹 필터를 성공적으로 생성한다")
        fun `creates masking filter successfully`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyRequestBody",
                args = mapOf(
                    "transformType" to "mask",
                    "maskPattern" to "***"
                )
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(ModifyRequestBodyGatewayFilter::class.java)
        }

        @Test
        @DisplayName("transformType이 누락된 경우 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException when transformType is missing`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyRequestBody",
                args = mapOf("contentType" to "application/json")
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Missing required arguments: [transformType]")
        }

        @Test
        @DisplayName("지원하지 않는 transformType에 대해 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException for unsupported transformType`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyRequestBody",
                args = mapOf("transformType" to "unsupported_type")
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
                .hasMessageContaining("Unsupported transformType: unsupported_type")
        }
    }

    @Nested
    @DisplayName("ModifyResponseBodyGatewayFilter 생성")
    inner class ModifyResponseBodyFilterCreation {

        @Test
        @DisplayName("소문자 변환 필터를 성공적으로 생성한다")
        fun `creates lowercase transform filter successfully`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyResponseBody",
                args = mapOf("transformType" to "lowercase")
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(ModifyResponseBodyGatewayFilter::class.java)
        }

        @Test
        @DisplayName("민감한 데이터 제거 필터를 성공적으로 생성한다")
        fun `creates sensitive data removal filter successfully`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyResponseBody",
                args = mapOf("transformType" to "remove_sensitive")
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(ModifyResponseBodyGatewayFilter::class.java)
        }

        @Test
        @DisplayName("JSON 변환 필터를 성공적으로 생성한다")
        fun `creates json transform filter successfully`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyResponseBody",
                args = mapOf(
                    "transformType" to "json_transform",
                    "removeFields" to "password,ssn,creditCard"
                )
            )

            // When
            val filter = factory.create(definition)

            // Then
            assertThat(filter).isInstanceOf(ModifyResponseBodyGatewayFilter::class.java)
        }

        @Test
        @DisplayName("Content-Type과 order를 포함한 필터를 성공적으로 생성한다")
        fun `creates filter with contentType and order successfully`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyResponseBody",
                args = mapOf(
                    "transformType" to "uppercase",
                    "contentType" to "application/json",
                    "order" to "100"
                ),
                order = 50
            )

            // When
            val filter = factory.create(definition) as ModifyResponseBodyGatewayFilter

            // Then
            assertThat(filter).isInstanceOf(ModifyResponseBodyGatewayFilter::class.java)
            assertThat(filter.getOrder()).isEqualTo(100) // args의 order가 우선
        }

        @Test
        @DisplayName("잘못된 Content-Type에 대해 FilterInstantiationException을 던진다")
        fun `throws FilterInstantiationException for invalid contentType`() {
            // Given
            val definition = FilterDefinition(
                type = "ModifyResponseBody",
                args = mapOf(
                    "transformType" to "uppercase",
                    "contentType" to "invalid-content-type"
                )
            )

            // When & Then
            assertThatThrownBy { factory.create(definition) }
                .isInstanceOf(FilterInstantiationException::class.java)
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
                type = "AddRequestHeader",
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
            assertThat(caughtException!!.message).contains("AddRequestHeader")
            assertThat(caughtException.args).isEqualTo(definition.args)
        }
    }
}