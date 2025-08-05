package dev.jazzybyte.lite.gateway.exception

/**
  * Predicate 클래스를 검색하거나 로드할 수 없을 때 발생하는 예외입니다.
  * 이 예외는 실패한 프레디케이트 검색 프로세스에 대한 자세한 정보를 제공합니다.
  *
  * @param message 검색 문제를 설명하는 오류 메시지
  * @param predicateName 검색할 수 없었던 Predicate 이름 (선택 사항)
  * @param packageName 오류가 발생한 동안 스캔 중이던 패키지 이름 (선택 사항)
  * @param cause 예외의 근본 원인 (선택 사항)
  */
class PredicateDiscoveryException(
    message: String,
    val predicateName: String? = null,
    val packageName: String? = null,
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, predicateName, packageName), cause) {

    companion object {
        private fun buildMessage(
            message: String,
            predicateName: String?,
            packageName: String?
        ): String {
            val context = when {
                predicateName != null && packageName != null -> 
                    " (predicate: '$predicateName', package: '$packageName')"
                predicateName != null -> 
                    " (predicate: '$predicateName')"
                packageName != null -> 
                    " (package: '$packageName')"
                else -> ""
            }
            
            return "Predicate discovery error$context: $message"
        }
    }
}