package dev.jazzybyte.lite.gateway.exception

/**
 * Filter 클래스를 검색하거나 로드할 수 없을 때 발생하는 예외입니다.
 * 이 예외는 실패한 필터 검색 프로세스에 대한 자세한 정보를 제공합니다.
 *
 * @param message 검색 문제를 설명하는 오류 메시지
 * @param filterName 검색할 수 없었던 Filter 이름 (선택 사항)
 * @param packageName 오류가 발생한 동안 스캔 중이던 패키지 이름 (선택 사항)
 * @param cause 예외의 근본 원인 (선택 사항)
 */
class FilterDiscoveryException(
    message: String,
    val filterName: String? = null,
    val packageName: String? = null,
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, filterName, packageName), cause) {

    companion object {
        private fun buildMessage(
            message: String,
            filterName: String?,
            packageName: String?
        ): String {
            val context = when {
                filterName != null && packageName != null ->
                    " (filter: '$filterName', package: '$packageName')"
                filterName != null ->
                    " (filter: '$filterName')"
                packageName != null ->
                    " (package: '$packageName')"
                else -> ""
            }

            return "Filter discovery error$context: $message"
        }
    }
}
