package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.config.FilterRegistry
import dev.jazzybyte.lite.gateway.exception.FilterDiscoveryException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FilterRegistry 테스트")
class FilterRegistryTest {

    @Test
    @DisplayName("Filter 클래스들이 성공적으로 발견되어야 함")
    fun `should discover filter classes successfully`() {

        // when
        val availableFilters = FilterRegistry.getAvailableFilterNames()

        // then
        assertThat(availableFilters).isNotEmpty()
        assertThat(FilterRegistry.getRegisteredFilterCount()).isGreaterThan(0)
    }

    @Test
    @DisplayName("등록된 Filter 이름으로 클래스를 조회할 수 있어야 함")
    fun `should be able to get filter class by name`() {

        // given
        val availableFilters = FilterRegistry.getAvailableFilterNames()
        val firstFilterName = availableFilters.first()

        // when
        val filterClass = FilterRegistry.getFilterClass(firstFilterName)

        // then
        assertThat(filterClass).isNotNull()
        assertThat(GatewayFilter::class.java.isAssignableFrom(filterClass!!)).isTrue()
    }

    @Test
    @DisplayName("존재하지 않는 Filter 이름으로 조회하면 예외가 발생해야 함")
    fun `should throw exception for non-existent filter name`() {

        // when & then
        assertThatThrownBy { FilterRegistry.getFilterClass("NonExistentFilter") }
            .isInstanceOf(FilterDiscoveryException::class.java)
            .hasMessageContaining("is not registered")
    }

    @Test
    @DisplayName("Filter 등록 여부를 확인할 수 있어야 함")
    fun `should be able to check if filter is registered`() {

        // given
        val availableFilters = FilterRegistry.getAvailableFilterNames()
        val firstFilterName = availableFilters.first()

        // when & then
        assertThat(FilterRegistry.isFilterRegistered(firstFilterName)).isTrue()
        assertThat(FilterRegistry.isFilterRegistered("NonExistentFilter")).isFalse()
    }

    @Test
    @DisplayName("필터 메타데이터를 조회할 수 있어야 함")
    fun `should be able to get filter metadata`() {

        // given
        val availableFilters = FilterRegistry.getAvailableFilterNames()
        val firstFilterName = availableFilters.first()

        // when
        val metadata = FilterRegistry.getFilterMetadata(firstFilterName)

        // then
        assertThat(metadata).isNotNull
        assertThat(metadata!!.className).isNotBlank()
        assertThat(metadata.simpleName).isNotBlank()
        assertThat(metadata.packageName).isNotBlank()
        assertThat(metadata.constructorCount).isGreaterThan(0)
        assertThat(metadata.registrationTime).isGreaterThan(0)
    }

    @Test
    @DisplayName("모든 필터 메타데이터를 조회할 수 있어야 함")
    fun `should be able to get all filter metadata`() {

        // when
        val allMetadata = FilterRegistry.getAllFilterMetadata()

        // then
        assertThat(allMetadata).isNotEmpty()
        assertThat(allMetadata.size).isEqualTo(FilterRegistry.getRegisteredFilterCount())
    }

    @Test
    @DisplayName("필터 이름 검증이 올바르게 작동해야 함")
    fun `should validate filter names correctly`() {

        // when & then
        assertThat(FilterRegistry.validateFilterName("ValidFilterName")).isTrue()
        assertThat(FilterRegistry.validateFilterName("Valid123")).isTrue()
        assertThat(FilterRegistry.validateFilterName("")).isFalse()
        assertThat(FilterRegistry.validateFilterName(" ")).isFalse()
        assertThat(FilterRegistry.validateFilterName("123Invalid")).isFalse()
        assertThat(FilterRegistry.validateFilterName("Invalid-Name")).isFalse()
        assertThat(FilterRegistry.validateFilterName("a".repeat(51))).isFalse()
    }

    @Test
    @DisplayName("필터 클래스 검증이 올바르게 작동해야 함")
    fun `should validate filter classes correctly`() {

        // given
        val availableFilters = FilterRegistry.getAvailableFilterNames()
        val firstFilterName = availableFilters.first()
        val filterClass = FilterRegistry.getFilterClass(firstFilterName)

        // when & then - 유효한 필터 클래스는 예외를 던지지 않아야 함
        FilterRegistry.validateFilterClass(filterClass)
    }
}
