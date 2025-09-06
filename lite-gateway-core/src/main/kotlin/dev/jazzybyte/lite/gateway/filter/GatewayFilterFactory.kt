package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter

/**
 * FilterDefinition을 기반으로 GatewayFilter 인스턴스를 생성하는 팩토리 인터페이스.
 */
interface GatewayFilterFactory {

    fun create(definition: FilterDefinition): GatewayFilter
}