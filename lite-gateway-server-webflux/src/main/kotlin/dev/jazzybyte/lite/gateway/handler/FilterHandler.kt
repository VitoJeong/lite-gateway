package dev.jazzybyte.lite.gateway.handler

import dev.jazzybyte.lite.gateway.client.WebFluxHttpClient
import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import dev.jazzybyte.lite.gateway.filter.GatewayContext
import dev.jazzybyte.lite.gateway.filter.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.webflux.DefaultGatewayFilterChain
import dev.jazzybyte.lite.gateway.handler.GatewayHandlerMapping.Companion.MATCHED_ROUTE_ATTRIBUTE
import dev.jazzybyte.lite.gateway.route.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebHandler
import reactor.core.publisher.Mono
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

class FilterHandler(
    private val webclient: WebFluxHttpClient,
    private val globalFilters: List<GatewayFilter> // GlobalFilter 주입
) : WebHandler {

    override fun handle(exchange: ServerWebExchange): Mono<Void> {
        val matchedRoute = exchange.attributes[MATCHED_ROUTE_ATTRIBUTE] as? Route
            ?: return handleNoRoute(exchange)

        // 1. GlobalFilter와 RouteFilter를 합친다.
        val allFilters = (globalFilters + matchedRoute.filters).distinct()

        // 2. WebFluxGatewayContext 어댑터를 생성한다.
        val gatewayContext = WebFluxGatewayContext(exchange, matchedRoute)

        // 3. 필터 체인을 생성하고 실행한다.
        val chain = DefaultGatewayFilterChain(allFilters, createFinalAction(matchedRoute))
        return chain.filter(gatewayContext)
    }

    /**
     * 모든 필터가 실행된 후 수행될 최종 액션 (프록시 요청)을 생성하는 함수
     */
    private fun createFinalAction(route: Route): (GatewayContext) -> Mono<Void> = { ctx ->
        val webfluxContext = ctx as WebFluxGatewayContext // 실제 exchange에 접근하기 위해 다운캐스팅
        val targetUri = route.uri.resolve(
            webfluxContext.exchange.request.uri.path + buildQueryString(webfluxContext.exchange.request)
        )
        log.info { "Forwarding request to: $targetUri" }
        webclient.forwardRequest(webfluxContext.exchange, targetUri)
    }

    private fun handleNoRoute(exchange: ServerWebExchange): Mono<Void> {
        log.warn { "No matched route found in exchange attributes." }

        val message = "No route matched for request: ${exchange.request.uri}"
        return writeTextResponse(exchange, HttpStatus.NOT_FOUND, message)
    }

    /**
     * 텍스트 기반 응답을 작성하는 유틸리티 메서드
     */
    private fun writeTextResponse(
        exchange: ServerWebExchange,
        status: HttpStatusCode,
        message: String
    ): Mono<Void> {
        val buffer = exchange.response.bufferFactory()
            .wrap(message.toByteArray(StandardCharsets.UTF_8))
        exchange.response.statusCode = status
        exchange.response.headers.contentType = MediaType.TEXT_PLAIN
        return exchange.response.writeWith(Mono.just(buffer))
    }

    /**
     * 요청의 쿼리 파라미터를 URL 인코딩하여 쿼리 문자열로 변환
     */
    private fun buildQueryString(request: ServerHttpRequest): String {
        val queryParams = request.queryParams

        if (queryParams.isEmpty()) {
            return ""
        } else {
            val sb = StringBuilder("?")
            queryParams.forEach { (key, values) ->
                values.forEach { value ->
                    sb.append("${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}&")
                }
            }
            return sb.removeSuffix("&").toString()
        }
    }
}
