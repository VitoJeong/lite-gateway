package dev.jazzybyte.lite.gateway.exception

/**
 * 필터 실행 중 오류가 발생했을 때 던져지는 예외
 * 이 예외는 필터 체인 실행 중 발생하는 런타임 오류에 대한 상세한 컨텍스트 정보를 제공한다.
 *
 * @param message 실행 오류를 설명하는 메시지
 * @param filterName 실행에 실패한 필터의 이름
 * @param routeId 필터가 실행된 라우트 ID (선택 사항)
 * @param requestId 요청 식별자 (선택 사항)
 * @param cause 예외의 근본 원인 (선택 사항)
 */
class FilterExecutionException(
    message: String,
    val filterName: String,
    val routeId: String? = null,
    val requestId: String? = null,
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, filterName, routeId, requestId), cause) {

    companion object {
        private fun buildMessage(
            message: String,
            filterName: String,
            routeId: String?,
            requestId: String?
        ): String {
            val contextParts = mutableListOf<String>()
            
            contextParts.add("filter: '$filterName'")
            
            routeId?.let { contextParts.add("route: '$it'") }
            requestId?.let { contextParts.add("request: '$it'") }
            
            val context = contextParts.joinToString(", ")
            
            return "Filter execution failed ($context): $message"
        }
    }
}