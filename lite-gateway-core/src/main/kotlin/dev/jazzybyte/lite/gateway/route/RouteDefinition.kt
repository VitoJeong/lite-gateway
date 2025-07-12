package dev.jazzybyte.lite.gateway.route

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import java.util.*

/**
 * 라우트 정의 클래스
 * 라우트의 ID, URI, 조건(프레디케이트) 등을 포함합니다.
 *
 * @property _id 라우트의 고유 식별자 (null일 경우 UUID 생성)
 * @property uri 라우트가 요청을 처리할 URI
 * @property predicates 요청이 이 라우트에 매칭되는지 판단하는 조건들
 * @property order 라우트의 우선순위 (0 이상)
 */
@Validated
data class RouteDefinition(

    private val _id: String? = null,

    @field:NotBlank(message = "uri는 필수 값입니다.")
    val uri: String,

//    @field:Valid
//    val predicates: List<PredicateDefinition> = emptyList(),

//    @field:Valid
//    val filters: List<RouteFilter> = emptyList(),

    @field:Min(0, message = "order는 0 이상이어야 합니다.")
    val order: Int = 0
) {

    private val log = KotlinLogging.logger {}

    private val id: String by lazy {
        if (_id.isNullOrBlank()) {
            log.warn { "Route ID is blank, generating a random UUID." }
            UUID.randomUUID()
        } else {
            _id
        }.toString()
    }

    override fun toString(): String {
        return "RouteDefinition(id='$id', uri='$uri', order=$order)"
    }
}