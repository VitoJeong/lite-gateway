package dev.jazzybyte.lite.gateway.integration

import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.ModifyRequestBodyGatewayFilter
import dev.jazzybyte.lite.gateway.filter.ModifyResponseBodyGatewayFilter
import dev.jazzybyte.lite.gateway.filter.webflux.WebfluxGatewayFilterFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("본문 수정 필터 통합 테스트")
class BodyModificationFilterIntegrationTest {

    private lateinit var factory: WebfluxGatewayFilterFactory

    @BeforeEach
    fun setUp() {
        factory = WebfluxGatewayFilterFactory()
    }

    @Test
    @DisplayName("YAML 설정으로 ModifyRequestBodyGatewayFilter를 생성할 수 있어야 한다")
    fun shouldCreateModifyRequestBodyFilterFromYamlConfig() {
        // Given - YAML 설정을 시뮬레이션
        val definition = FilterDefinition(
            type = "ModifyRequestBody",
            args = mapOf(
                "transformType" to "uppercase",
                "contentType" to "application/json"
            ),
            order = 10
        )

        // When
        val filter = factory.create(definition)

        // Then
        assertNotNull(filter)
        assertTrue(filter is ModifyRequestBodyGatewayFilter)
    }

    @Test
    @DisplayName("YAML 설정으로 ModifyResponseBodyGatewayFilter를 생성할 수 있어야 한다")
    fun shouldCreateModifyResponseBodyFilterFromYamlConfig() {
        // Given - YAML 설정을 시뮬레이션
        val definition = FilterDefinition(
            type = "ModifyResponseBody",
            args = mapOf(
                "transformType" to "mask",
                "maskPattern" to "***",
                "contentType" to "application/json",
                "order" to "100"
            ),
            order = 50
        )

        // When
        val filter = factory.create(definition)

        // Then
        assertNotNull(filter)
        assertTrue(filter is ModifyResponseBodyGatewayFilter)
        assertEquals(100, (filter as ModifyResponseBodyGatewayFilter).getOrder())
    }

    @Test
    @DisplayName("민감한 데이터 제거 필터를 생성할 수 있어야 한다")
    fun shouldCreateSensitiveDataRemovalFilter() {
        // Given
        val definition = FilterDefinition(
            type = "ModifyResponseBody",
            args = mapOf(
                "transformType" to "remove_sensitive"
            )
        )

        // When
        val filter = factory.create(definition)

        // Then
        assertNotNull(filter)
        assertTrue(filter is ModifyResponseBodyGatewayFilter)
    }

    @Test
    @DisplayName("JSON 필드 제거 필터를 생성할 수 있어야 한다")
    fun shouldCreateJsonFieldRemovalFilter() {
        // Given
        val definition = FilterDefinition(
            type = "ModifyRequestBody",
            args = mapOf(
                "transformType" to "json_transform",
                "removeFields" to "password,ssn,creditCard,apiKey"
            )
        )

        // When
        val filter = factory.create(definition)

        // Then
        assertNotNull(filter)
        assertTrue(filter is ModifyRequestBodyGatewayFilter)
    }

    @Test
    @DisplayName("여러 필터 타입을 동시에 생성할 수 있어야 한다")
    fun shouldCreateMultipleFilterTypes() {
        // Given
        val requestFilterDef = FilterDefinition(
            type = "ModifyRequestBody",
            args = mapOf("transformType" to "lowercase")
        )
        val responseFilterDef = FilterDefinition(
            type = "ModifyResponseBody",
            args = mapOf("transformType" to "uppercase")
        )

        // When
        val requestFilter = factory.create(requestFilterDef)
        val responseFilter = factory.create(responseFilterDef)

        // Then
        assertTrue(requestFilter is ModifyRequestBodyGatewayFilter)
        assertTrue(responseFilter is ModifyResponseBodyGatewayFilter)
    }
}