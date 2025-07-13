package dev.jazzybyte.lite.gateway.config

import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.DispatcherHandler

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
@ConditionalOnClass(DispatcherHandler::class)
class LiteGatewayAutoConfiguration {


}