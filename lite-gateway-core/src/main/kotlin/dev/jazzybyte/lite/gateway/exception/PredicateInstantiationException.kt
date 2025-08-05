package dev.jazzybyte.lite.gateway.exception

/**
 * 프레디케이트 인스턴스를 생성할 수 없을 때 발생하는 예외입니다.
 * 이 예외는 인스턴스화에 실패한 프레디케이트에 대한 자세한 정보를 제공합니다.
 * 여기에는 라우트 컨텍스트와 프레디케이트 인수도 포함됩니다.
 *
 * @param message 인스턴스화 문제를 설명하는 오류 메시지
 * @param routeId 인스턴스화에 실패한 프레디케이트를 포함하는 라우트의 ID
 * @param predicateName 인스턴스화에 실패한 프레디케이트의 이름
 * @param predicateArgs 프레디케이트 생성자에 제공된 인수 (선택 사항)
 * @param cause 예외의 근본 원인 (선택 사항)
 */
class PredicateInstantiationException(
    message: String,
    val routeId: String,
    val predicateName: String,
    val predicateArgs: Array<String>? = null,
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, routeId, predicateName, predicateArgs), cause) {

    companion object {
        private fun buildMessage(
            message: String,
            routeId: String,
            predicateName: String,
            predicateArgs: Array<String>?
        ): String {
            val argsInfo = if (predicateArgs != null && predicateArgs.isNotEmpty()) {
                " with arguments [${predicateArgs.joinToString(", ")}]"
            } else {
                ""
            }
            
            return "Failed to instantiate predicate '$predicateName' for route '$routeId'$argsInfo: $message"
        }
    }
}