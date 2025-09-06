package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.config.FilterRegistry
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
    @DisplayName("존재하지 않는 Filter 이름으로 조회하면 null을 반환해야 함")
    fun `should return null for non-existent filter name`() {
        
        // when
        val filterClass = FilterRegistry.getFilterClass("NonExistentFilter")

        // then
        assertThat(filterClass).isNull()
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
}
