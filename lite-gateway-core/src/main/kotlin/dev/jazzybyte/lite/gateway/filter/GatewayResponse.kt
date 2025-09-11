package dev.jazzybyte.lite.gateway.filter

/**
 * 프레임워크 독립적인 응답 인터페이스.
 * 필터가 응답의 상태 코드, 헤더 등을 조작할 수 있도록 한다.
 */
interface GatewayResponse {
    /**
     * HTTP 응답 상태 코드를 가져오거나 설정한다.
     */
    var statusCode: Int

    /**
     * 응답 헤더를 설정한다.
     *
     * @param name 헤더 이름
     * @param value 헤더 값
     */
    fun setHeader(name: String, value: String)

    /**
     * 응답을 변경하기 위한 빌더를 반환한다.
     */
    fun mutate(): GatewayResponseBuilder
}

/**
 * GatewayResponse를 빌드하거나 변경하기 위한 인터페이스.
 */
interface GatewayResponseBuilder {
    /**
     * 응답 상태 코드를 설정한다.
     */
    fun statusCode(statusCode: Int): GatewayResponseBuilder

    /**
     * 헤더를 추가하거나 교체한다.
     */
    fun header(name: String, vararg values: String): GatewayResponseBuilder

    /**
     * 변경된 GatewayResponse 인스턴스를 빌드한다.
     */
    fun build(): GatewayResponse
}
