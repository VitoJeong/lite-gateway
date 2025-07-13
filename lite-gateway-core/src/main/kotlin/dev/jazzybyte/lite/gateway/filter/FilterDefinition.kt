package dev.jazzybyte.lite.gateway.filter

import jakarta.validation.constraints.NotEmpty
import org.springframework.validation.annotation.Validated

/**
 * 필터 정의를 위한 데이터 클래스.
 *
 * @property name 필터의 이름 (e.g., "AddRequestHeader").
 * @property args 필터에 필요한 인자 맵 (e.g., "name" to "X-Request-ID").
 */
@Validated
data class FilterDefinition(
    @field:NotEmpty
    val name: String,
    val args: Map<String, String> = mutableMapOf()
)
