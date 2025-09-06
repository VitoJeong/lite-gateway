package dev.jazzybyte.lite.gateway.filter

import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated

/**
 * 필터 정의를 위한 데이터 클래스.
 *
 * @property type 필터의 유형(e.g., "AddRequestHeader").
 * @property args 필터에 필요한 인자 맵 (e.g., "name" to "X-Request-ID").
 * @property order 필터의 실행 순서 (낮은 값이 먼저 실행됨, 기본값: 0).
 */
@Validated
data class FilterDefinition(
    @field:NotBlank(message = "Filter name cannot be blank")
    val type: String,
    // ConfigurationProperties가 바인딩할 대상
    val args: Map<String, String> = emptyMap(),
    val order: Int = 0
) {

    init {
        // 순서 값 검증 - 합리적인 범위 내에서만 허용
        require(order in Int.MIN_VALUE..Int.MAX_VALUE) {
            "Filter order must be within valid integer range, but was: $order"
        }
    }

    override fun toString(): String {
        return "FilterDefinition(type='$type', args=$args, order=$order)"
    }
}