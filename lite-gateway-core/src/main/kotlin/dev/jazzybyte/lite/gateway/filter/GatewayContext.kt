package dev.jazzybyte.lite.gateway.filter

/**
 * 프레임워크 독립적인 요청/응답 컨텍스트 인터페이스.
 * 필터가 요청 및 응답 정보에 접근하고 조작할 수 있도록 한다.
 */
interface GatewayContext {
    /**
     * 현재 요청 정보를 반환한다.
     */
    val request: GatewayRequest

    /**
     * 현재 응답 정보를 반환한다.
     */
    val response: GatewayResponse

    /**
     * 컨텍스트에 저장된 속성 값을 가져온다.
     *
     * @param name 속성 이름
     * @return 속성 값, 없으면 null
     */
    fun getAttribute(name: String): Any?

    /**
     * 컨텍스트에 속성을 저장한다.
     *
     * @param name 속성 이름
     * @param value 저장할 값
     */
    fun putAttribute(name: String, value: Any)

    /**
     * 컨텍스트를 변경하기 위한 빌더를 반환한다.
     */
    fun mutate(): GatewayContextBuilder
}

/**
 * GatewayContext를 빌드하거나 변경하기 위한 인터페이스.
 */
interface GatewayContextBuilder {
    /**
     * 요청을 설정한다.
     */
    fun request(request: GatewayRequest): GatewayContextBuilder

    /**
     * 응답을 설정한다.
     */
    fun response(response: GatewayResponse): GatewayContextBuilder

    /**
     * 속성을 설정한다.
     */
    fun attribute(name: String, value: Any): GatewayContextBuilder

    /**
     * 변경된 GatewayContext 인스턴스를 빌드한다.
     */
    fun build(): GatewayContext
}
