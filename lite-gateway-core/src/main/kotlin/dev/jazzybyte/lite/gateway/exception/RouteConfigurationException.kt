package dev.jazzybyte.lite.gateway.exception

/**
* 라우트 정의와 관련된 구성 오류가 있을 때 발생하는 예외이다.
* 이 예외는 실패한 라우트 구성에 대한 자세한 정보를 제공한다.
*
* @param message 구성 문제를 설명하는 오류 메시지
* @param routeId 구성 오류가 발생한 라우트의 ID (선택 사항)
* @param cause 예외의 근본 원인 (선택 사항)
*/
class RouteConfigurationException(
    message: String,
    val routeId: String? = null,
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, routeId), cause) {

    companion object {
        private fun buildMessage(message: String, routeId: String?): String {
            return if (routeId != null) {
                "Route configuration error for route '$routeId': $message"
            } else {
                "Route configuration error: $message"
            }
        }
    }
}