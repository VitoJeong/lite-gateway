package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.client.WebFluxHttpClient
import dev.jazzybyte.lite.gateway.handler.FilterHandler
import dev.jazzybyte.lite.gateway.handler.GatewayHandlerMapping
import dev.jazzybyte.lite.gateway.route.RouteLocator
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Lite Gateway의 자동 설정을 위한 클래스입니다.
 *
 * 이 클래스는 Lite Gateway의 설정을 로드하고, 필요한 빈들을 등록합니다.
 * 현재는 별도의 빈 등록 로직은 없지만, 필요시 추가할 수 있습니다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = LiteGatewayConfigProperties.PREFIX,
    name = ["enabled"],
    matchIfMissing = true)
@EnableConfigurationProperties(LiteGatewayConfigProperties::class)
@AutoConfigureBefore(HttpHandlerAutoConfiguration::class, WebFluxAutoConfiguration::class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
class LiteGatewayAutoConfiguration(
    private val properties: LiteGatewayConfigProperties
) {

    @Bean
    fun routeLocator() = RouteLocatorFactory.create(properties.routes)

    @Bean
    fun webClient() = WebClientFactory.create(properties.httpClient)

    @Bean
    fun filterHandler(webClient: WebFluxHttpClient) = FilterHandler(webClient)

    @Bean
    fun gatewayHandlerMapping(
        routeLocator: RouteLocator,
        filterHandler: FilterHandler,
    ): GatewayHandlerMapping {
        return GatewayHandlerMapping(routeLocator, filterHandler)
    }
}