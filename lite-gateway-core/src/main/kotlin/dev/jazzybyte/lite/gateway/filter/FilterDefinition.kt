package dev.jazzybyte.lite.gateway.filter

import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated

/**
 * 필터 정의를 위한 데이터 클래스.
 *
 * @property type 필터의 유형(e.g., "AddRequestHeader").
 * @property args 필터에 필요한 인자 맵 (e.g., "name" to "X-Request-ID").
 */
@Validated
data class FilterDefinition(
    @field:NotBlank(message = "Filter name cannot be blank")
    val type: String,
    // ConfigurationProperties가 바인딩할 대상
    val args: Map<String, String> = emptyMap()
) {

    override fun toString(): String {
        return "FilterDefinition(name='$type', args=$args)"
    }
}