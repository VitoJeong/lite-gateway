package dev.jazzybyte.lite.gateway.exception

/**
 * 필터 설정이 잘못되었거나 유효하지 않을 때 발생하는 예외
 * 이 예외는 필터 설정 검증 과정에서 발견된 문제에 대한 상세한 정보를 제공한다.
 *
 * @param message 설정 오류를 설명하는 메시지
 * @param filterType 설정 오류가 발생한 필터 타입
 * @param configKey 문제가 있는 설정 키 (선택 사항)
 * @param configValue 문제가 있는 설정 값 (선택 사항)
 * @param cause 예외의 근본 원인 (선택 사항)
 */
class FilterConfigurationException(
    message: String,
    val filterType: String,
    val configKey: String? = null,
    val configValue: String? = null,
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, filterType, configKey, configValue), cause) {

    companion object {
        private fun buildMessage(
            message: String,
            filterType: String,
            configKey: String?,
            configValue: String?
        ): String {
            val contextParts = mutableListOf<String>()
            
            contextParts.add("filter: '$filterType'")
            
            if (configKey != null) {
                val keyValue = if (configValue != null) {
                    "$configKey='$configValue'"
                } else {
                    configKey
                }
                contextParts.add("config: $keyValue")
            }
            
            val context = contextParts.joinToString(", ")
            
            return "Filter configuration error ($context): $message"
        }
    }
}