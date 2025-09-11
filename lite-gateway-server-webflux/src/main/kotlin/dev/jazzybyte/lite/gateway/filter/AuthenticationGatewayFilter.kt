package dev.jazzybyte.lite.gateway.filter

import reactor.core.publisher.Mono

/**
 * 인증을 처리하는 Critical 필터 예시
 * 
 * 이 필터는 요청의 Authorization 헤더를 검증하고,
 * 인증에 실패할 경우 401 Unauthorized 응답을 반환한다.
 */
class AuthenticationGatewayFilter(
    private val order: Int = 100
) : GatewayFilter, CriticalFilter, OrderedGatewayFilter {

    override fun getOrder(): Int = order

    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        return Mono.fromCallable {
            // 인증 로직 수행
            val authHeader = context.request.headers["Authorization"]?.firstOrNull()
            
            if (authHeader == null || !isValidToken(authHeader)) {
                throw RuntimeException("Invalid or missing authentication token")
            }
            
            // 인증 성공 시 사용자 정보를 컨텍스트에 추가
            context.putAttribute("authenticated", true)
            context.putAttribute("userId", extractUserId(authHeader))
        }
        .then(chain.filter(context))
    }

    override fun getFailureStatusCode(): Int = 401

    override fun getFailureMessage(cause: Throwable): String = "Authentication failed"

    /**
     * 토큰 유효성을 검증한다.
     * 실제 구현에서는 JWT 검증, 데이터베이스 조회 등을 수행한다.
     */
    private fun isValidToken(authHeader: String): Boolean {
        // 간단한 예시: Bearer 토큰 형식 확인
        return authHeader.startsWith("Bearer ") && authHeader.length > 7
    }

    /**
     * 토큰에서 사용자 ID를 추출한다.
     */
    private fun extractUserId(authHeader: String): String {
        // 간단한 예시: 토큰의 마지막 부분을 사용자 ID로 사용
        return authHeader.substringAfter("Bearer ").take(10)
    }
}