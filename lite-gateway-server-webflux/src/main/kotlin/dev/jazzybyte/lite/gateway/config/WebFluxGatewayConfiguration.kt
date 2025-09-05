package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.client.WebFluxHttpClient
import dev.jazzybyte.lite.gateway.handler.FilterHandler
import dev.jazzybyte.lite.gateway.handler.GatewayHandlerMapping
import dev.jazzybyte.lite.gateway.route.RouteLocator
import dev.jazzybyte.lite.gateway.route.RouteLocatorFactory
import dev.jazzybyte.lite.gateway.filter.webflux.WebfluxGatewayFilterFactory
import dev.jazzybyte.lite.gateway.route.WebfluxRouteLocatorFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * WebFlux 기반 Gateway의 구체적인 빈 설정을 담당하는 클래스이다.
 * LiteGatewayAutoConfiguration이 활성화된 후에 실행된다.
 */
@Configuration
@AutoConfigureAfter(LiteGatewayAutoConfiguration::class)
@ConditionalOnBean(LiteGatewayAutoConfiguration::class)
class WebFluxGatewayConfiguration(
    private val properties: LiteGatewayConfigProperties
) {

    @Bean
    @ConditionalOnMissingBean
    fun gatewayFilterFactory(): WebfluxGatewayFilterFactory {
        return WebfluxGatewayFilterFactory()
    }

    @Bean
    @ConditionalOnMissingBean
    fun routeLocatorFactory(gatewayFilterFactory: WebfluxGatewayFilterFactory): RouteLocatorFactory {
        return WebfluxRouteLocatorFactory(gatewayFilterFactory)
    }

    @Bean
    @ConditionalOnMissingBean
    fun routeLocator(factory: RouteLocatorFactory): RouteLocator {
        return factory.create(properties.routes)
    }

    @Bean
    @ConditionalOnMissingBean
    fun webClient(): WebFluxHttpClient {
       val httpClient = properties.httpClient
        return WebFluxHttpClient(
            maxConnections = httpClient.maxConnections,
            connectionTimeout = httpClient.connectionTimeout,
            maxHeaderSize = httpClient.maxHeaderSize,
            acquireTimeout = httpClient.acquireTimeout
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun filterHandler(webClient: WebFluxHttpClient): FilterHandler {
        return FilterHandler(webClient, emptyList()) // Provide an empty list of global filters
    }

    @Bean
    @ConditionalOnMissingBean
    fun gatewayHandlerMapping(
        routeLocator: RouteLocator,
        filterHandler: FilterHandler,
    ): GatewayHandlerMapping {
        return GatewayHandlerMapping(routeLocator, filterHandler)
    }
}