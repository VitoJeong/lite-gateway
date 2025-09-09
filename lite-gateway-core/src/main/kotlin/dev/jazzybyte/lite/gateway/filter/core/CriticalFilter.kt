package dev.jazzybyte.lite.gateway.filter.core

/**
 * 중요한 필터임을 나타내는 마커 인터페이스
 * 
 * 이 인터페이스를 구현하는 필터는 실행 실패 시 전체 요청 처리를 중단시키고
 * 적절한 HTTP 오류 응답을 반환한다.
 * 
 * 예시:
 * - 인증 필터 (실패 시 401 Unauthorized)
 * - 인가 필터 (실패 시 403 Forbidden)
 * - 속도 제한 필터 (실패 시 429 Too Many Requests)
 */
interface CriticalFilter {
    
    /**
     * 필터 실행 실패 시 반환할 HTTP 상태 코드를 정의한다.
     * 
     * @return HTTP 상태 코드 (기본값: 500 Internal Server Error)
     */
    fun getFailureStatusCode(): Int = 500
    
    /**
     * 필터 실행 실패 시 반환할 오류 메시지를 정의한다.
     * 
     * @param cause 실패 원인 예외
     * @return 클라이언트에게 반환할 오류 메시지
     */
    fun getFailureMessage(cause: Throwable): String = "Filter execution failed"
}