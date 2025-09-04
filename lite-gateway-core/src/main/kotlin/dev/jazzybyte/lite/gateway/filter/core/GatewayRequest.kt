package dev.jazzybyte.lite.gateway.filter.core

/**
 * 프레임워크 독립적인 요청 인터페이스.
 * 필터가 요청의 경로, 메서드, 헤더 등에 접근할 수 있도록 한다.
 */
interface GatewayRequest {
    /**
     * 요청 경로를 반환한다.
     */
    val path: String

    /**
     * HTTP 메서드를 반환합니다 (예: "GET", "POST").
     */
    val method: String

    /**
     * 요청 헤더 맵을 반환한다.
     * 키는 헤더 이름, 값은 해당 헤더의 값 목록이다.
     */
    val headers: Map<String, List<String>>

    /**
     * 요청을 변경하기 위한 빌더를 반환한다.
     */
    fun mutate(): GatewayRequestBuilder
}

/**
 * GatewayRequest를 빌드하거나 변경하기 위한 인터페이스.
 */
interface GatewayRequestBuilder {
    /**
     * 헤더를 추가하거나 교체한다.
     */
    fun header(name: String, vararg values: String): GatewayRequestBuilder

    /**
     * 변경된 GatewayRequest 인스턴스를 빌드한다.
     */
    fun build(): GatewayRequest
}
