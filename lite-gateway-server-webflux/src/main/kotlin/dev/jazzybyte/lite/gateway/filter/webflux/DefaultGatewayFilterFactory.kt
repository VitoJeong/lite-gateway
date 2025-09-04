package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.filter.AddRequestFilter
import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.GatewayFilterFactory
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import org.springframework.stereotype.Component

/**
 * FilterDefinition을 기반으로 GatewayFilter 인스턴스를 생성하는 기본 팩토리 구현체.
 * 등록된 필터 유형에 따라 적절한 GatewayFilter를 생성한다.
 */
@Component
class DefaultGatewayFilterFactory : GatewayFilterFactory {

    // 필터 유형과 해당 필터를 생성하는 람다 함수를 매핑한다.
    // 실제 애플리케이션에서는 이 맵을 동적으로 구성하거나, Spring의 ApplicationContext를 통해 필터 빈을 주입받을 수 있다.
    private val filterFactories: Map<String, (Map<String, String>) -> GatewayFilter> = mapOf(
        "AddRequestHeader" to { args ->
            val headerName = args["headerName"]
            val headerValue = args["headerValue"]
            if (headerName != null && headerValue != null) {
                AddRequestFilter(mapOf(headerName to headerValue))
            } else {
                throw IllegalArgumentException("AddRequestHeader filter requires 'headerName' and 'headerValue' arguments.")
            }
        }
        // TODO: 다른 필터 유형에 대한 팩토리 함수를 여기에 추가
    )

    override fun create(definition: FilterDefinition): GatewayFilter {
        val factory = filterFactories[definition.type]
            ?: throw IllegalArgumentException("Unknown filter type: ${definition.type}")
        return factory(definition.args)
    }
}
