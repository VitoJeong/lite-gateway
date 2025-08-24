package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.config.PredicateRegistry
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PredicateRegistry 테스트")
class WebFluxPredicateRegistryTest {

    private lateinit var predicateRegistry: PredicateRegistry

    @BeforeEach
    fun setUp() {
        predicateRegistry = PredicateRegistry()
    }

    @Test
    @DisplayName("Predicate 클래스들이 성공적으로 발견되어야 함")
    fun `should discover predicate classes successfully`() {
        // when
        val availablePredicates = predicateRegistry.getAvailablePredicateNames()
        
        // then
        assertThat(availablePredicates).isNotEmpty()
        assertThat(predicateRegistry.getRegisteredPredicateCount()).isGreaterThan(0)
    }

    @Test
    @DisplayName("등록된 Predicate 이름으로 클래스를 조회할 수 있어야 함")
    fun `should be able to get predicate class by name`() {
        // given
        val availablePredicates = predicateRegistry.getAvailablePredicateNames()
        val firstPredicateName = availablePredicates.first()
        
        // when
        val predicateClass = predicateRegistry.getPredicateClass(firstPredicateName)
        
        // then
        assertThat(predicateClass).isNotNull()
        assertThat(RoutePredicate::class.java.isAssignableFrom(predicateClass!!)).isTrue()
    }

    @Test
    @DisplayName("존재하지 않는 Predicate 이름으로 조회하면 null을 반환해야 함")
    fun `should return null for non-existent predicate name`() {
        // when
        val predicateClass = predicateRegistry.getPredicateClass("NonExistentPredicate")
        
        // then
        assertThat(predicateClass).isNull()
    }

    @Test
    @DisplayName("Predicate 등록 여부를 확인할 수 있어야 함")
    fun `should be able to check if predicate is registered`() {
        // given
        val availablePredicates = predicateRegistry.getAvailablePredicateNames()
        val firstPredicateName = availablePredicates.first()
        
        // when & then
        assertThat(predicateRegistry.isPredicateRegistered(firstPredicateName)).isTrue()
        assertThat(predicateRegistry.isPredicateRegistered("NonExistentPredicate")).isFalse()
    }
}