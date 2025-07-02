package dev.jazzybyte.lite.gateway.handler

import dev.jazzybyte.lite.gateway.route.RouteLocator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.reactive.handler.AbstractHandlerMapping
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


private val log = KotlinLogging.logger {}

/**
 * GatewayHandlerMapping 클래스는 요청을 처리할 핸들러를 결정하는 역할을 합니다.
 *
 * @property routeLocator 요청에 대한 라우팅 정보를 제공하는 RouteLocator
 * @property filterHandler 요청 필터링을 처리하는 FilterHandler
 */
class GatewayHandlerMapping(
    private val routeLocator: RouteLocator,
    private val filterHandler: FilterHandler,
) : AbstractHandlerMapping() {

    init {
        order = 0 // 핸들러 매핑의 우선순위를 설정합니다. 낮을수록 우선순위가 높습니다.
    }

    companion object {
        const val MATCHED_ROUTE_ATTRIBUTE = "matchedRoute"
    }

    /**
     * 요청에 맞는 핸들러를 반환합니다.
     */
    override fun getHandlerInternal(exchange: ServerWebExchange): Mono<FilterHandler> = resolveHandler(exchange)

    internal fun resolveHandler(exchange: ServerWebExchange): Mono<FilterHandler> {
        log.info { "Resolving handler for request: ${exchange.request.path}" }

        return routeLocator.locate(exchange)
            .doOnNext {
                log.info { "Matched route: ${it.id} for request: ${exchange.request.path}" }
                exchange.attributes[MATCHED_ROUTE_ATTRIBUTE] = it
            }
            .thenReturn(filterHandler)
            .switchIfEmpty(Mono.error(IllegalArgumentException("No matching route found for ${exchange.request.path}")))
    }
}