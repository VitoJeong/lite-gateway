package dev.jazzybyte.lite.gateway.exception

/**
 * 필터 인스턴스를 생성할 수 없을 때 발생하는 예외
 */
class FilterInstantiationException(
    message: String,
    filterType: String,
    val args: Map<String, String>? = null,
    cause: Throwable? = null,
) : RuntimeException(buildMessage(message, filterType, args), cause) {

    companion object {
        private fun buildMessage(
            message: String,
            filterType: String,
            args: Map<String, String>?,
        ): String {
            val argsString = args?.takeIf { it.isNotEmpty() }
                ?.entries?.joinToString(", ") { "${it.key}=${it.value}" }
                ?.let { " with arguments [$it]" } ?: ""

            return "Failed to instantiate filter '$filterType' with args=($argsString): $message"
        }
    }
}