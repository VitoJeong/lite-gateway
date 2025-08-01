package dev.jazzybyte.lite.gateway.route

import jakarta.validation.constraints.NotEmpty
import org.springframework.validation.annotation.Validated

/**
 * 라우트 조건(Predicate)을 정의하는 클래스.
 * 예: Path, Method, Header 등
 */
@Validated
data class PredicateDefinition(
    @field:NotEmpty
    val name: String,
    // ConfigurationProperties가 바인딩할 대상
    val args: String = ""
) {
    val parsedArgs: Array<String> by lazy {
        parseArgs(args)
    }

    private fun parseArgs(args: String): Array<String> {
        return if (args.isEmpty()) {
            emptyArray()
        } else {
            args.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
        }
    }

    override fun toString(): String {
        return "PredicateDefinition(name='$name', args=$parsedArgs)"
    }
}