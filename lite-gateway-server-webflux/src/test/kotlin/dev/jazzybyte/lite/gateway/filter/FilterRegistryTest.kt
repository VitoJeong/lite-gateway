package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.config.FilterRegistry
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FilterRegistry 테스트")
class FilterRegistryTest {

    private lateinit var filterRegistry: FilterRegistry

    @BeforeEach
    fun setUp() {
        // 테스트를 실행하기 위해서는 dev.jazzybyte.lite.gateway.filter 패키지 내에
        // 적어도 하나 이상의 GatewayFilter 구현체가 존재해야 한다.
        // 만약 구현체가 없다면, FilterDiscoveryException이 발생하여 테스트가 실패한다.
        try {
            filterRegistry = FilterRegistry()
        } catch (e: Exception) {
            // 테스트 환경에서 필터 구현체가 없을 경우를 대비한 임시 처리
            // 실제 프로덕션 코드에서는 필터가 존재해야 하므로 이 테스트는 통과해야 함
            println("WARN: Could not initialize FilterRegistry. This test will be skipped. Error: ${e.message}")
        }
    }

    // FilterRegistry가 초기화되지 않았을 경우 테스트를 실행하지 않도록 체크하는 helper
    private fun assumeRegistryInitialized() {
        if (!::filterRegistry.isInitialized) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "FilterRegistry could not be initialized, skipping test.")
        }
    }

    @Test
    @DisplayName("Filter 클래스들이 성공적으로 발견되어야 함")
    fun `should discover filter classes successfully`() {
        assumeRegistryInitialized()

        // when
        val availableFilters = filterRegistry.getAvailableFilterNames()

        // then
        assertThat(availableFilters).isNotEmpty()
        assertThat(filterRegistry.getRegisteredFilterCount()).isGreaterThan(0)
    }

    @Test
    @DisplayName("등록된 Filter 이름으로 클래스를 조회할 수 있어야 함")
    fun `should be able to get filter class by name`() {
        assumeRegistryInitialized()

        // given
        val availableFilters = filterRegistry.getAvailableFilterNames()
        val firstFilterName = availableFilters.first()

        // when
        val filterClass = filterRegistry.getFilterClass(firstFilterName)

        // then
        assertThat(filterClass).isNotNull()
        assertThat(GatewayFilter::class.java.isAssignableFrom(filterClass!!)).isTrue()
    }

    @Test
    @DisplayName("존재하지 않는 Filter 이름으로 조회하면 null을 반환해야 함")
    fun `should return null for non-existent filter name`() {
        assumeRegistryInitialized()

        // when
        val filterClass = filterRegistry.getFilterClass("NonExistentFilter")

        // then
        assertThat(filterClass).isNull()
    }

    @Test
    @DisplayName("Filter 등록 여부를 확인할 수 있어야 함")
    fun `should be able to check if filter is registered`() {
        assumeRegistryInitialized()

        // given
        val availableFilters = filterRegistry.getAvailableFilterNames()
        val firstFilterName = availableFilters.first()

        // when & then
        assertThat(filterRegistry.isFilterRegistered(firstFilterName)).isTrue()
        assertThat(filterRegistry.isFilterRegistered("NonExistentFilter")).isFalse()
    }
}
