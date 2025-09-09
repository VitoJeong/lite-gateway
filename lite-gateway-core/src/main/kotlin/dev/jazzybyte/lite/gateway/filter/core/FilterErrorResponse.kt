package dev.jazzybyte.lite.gateway.filter.core

import java.time.Instant

/**
 * RFC7807 Problem Details for HTTP APIs 표준을 따르는 필터 오류 응답 모델
 * 
 * @param type 문제 유형을 식별하는 URI 참조 (선택 사항)
 * @param title 문제에 대한 간단한 설명
 * @param status HTTP 상태 코드
 * @param detail 문제에 대한 상세한 설명 (선택 사항)
 * @param instance 문제가 발생한 특정 인스턴스를 식별하는 URI 참조 (선택 사항)
 * @param timestamp 오류 발생 시각
 * @param filterName 오류가 발생한 필터 이름
 * @param routeId 오류가 발생한 라우트 ID (선택 사항)
 * @param requestId 요청 식별자 (선택 사항)
 */
data class FilterErrorResponse(
    val type: String? = null,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val timestamp: Instant = Instant.now(),
    val filterName: String,
    val routeId: String? = null,
    val requestId: String? = null
) {
    companion object {
        /**
         * 필터 실행 오류에 대한 표준 오류 응답을 생성한다.
         */
        fun fromFilterExecutionException(
            exception: dev.jazzybyte.lite.gateway.exception.FilterExecutionException,
            status: Int = 500,
            title: String = "Filter Execution Error"
        ): FilterErrorResponse {
            return FilterErrorResponse(
                type = "about:blank",
                title = title,
                status = status,
                detail = exception.message,
                filterName = exception.filterName,
                routeId = exception.routeId,
                requestId = exception.requestId
            )
        }
        
        /**
         * 일반적인 필터 오류에 대한 표준 오류 응답을 생성한다.
         */
        fun fromException(
            filterName: String,
            exception: Throwable,
            status: Int = 500,
            title: String = "Filter Error",
            routeId: String? = null,
            requestId: String? = null
        ): FilterErrorResponse {
            return FilterErrorResponse(
                type = "about:blank",
                title = title,
                status = status,
                detail = exception.message ?: "Unknown error occurred",
                filterName = filterName,
                routeId = routeId,
                requestId = requestId
            )
        }
    }
}