package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import org.springframework.validation.annotation.Validated
import java.util.*

/**
 * `application.yml` 또는 `application.properties`에서 라우트 구성을 바인딩하기 위한 데이터 클래스.
 *
 * @property id 라우트의 고유 식별자. 비어있을 경우 UUID가 자동으로 생성됩니다.
 * @property uri 라우팅할 대상 URI.
 * @property predicates 요청을 이 라우트로 매칭시킬 조건 목록.
 * @property filters 이 라우트에 적용될 필터 목록.
 * @property order 라우트의 우선순위. 낮을수록 먼저 평가됩니다.
 */
@Validated
data class RouteDefinition(
    val id: String = UUID.randomUUID().toString(),

    @field:NotEmpty(message = "URI는 비어 있을 수 없습니다.")
    val uri: String,

    @field:Valid
    val predicates: List<PredicateDefinition> = emptyList(),

    @field:Valid
    val filters: List<FilterDefinition> = emptyList(),

    @field:Min(0, message = "order는 0 이상이어야 합니다.")
    val order: Int = 0
) {
    override fun toString(): String {
        return "RouteDefinition(id='$id', uri='$uri', predicates=$predicates, filters=$filters, order=$order)"
    }
}