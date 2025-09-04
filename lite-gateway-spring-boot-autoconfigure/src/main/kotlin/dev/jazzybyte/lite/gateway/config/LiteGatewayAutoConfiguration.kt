package dev.jazzybyte.lite.gateway.config

import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Lite Gateway의 자동 설정을 위한 클래스이다.
 * 
 * 이 클래스는 자동 설정 조건만 정의하고, 실제 빈 등록은 WebFluxGatewayConfiguration에서 수행한다.
 * WebFluxGatewayConfiguration은 별도의 AutoConfiguration으로 등록되어 자동으로 로드된다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = LiteGatewayConfigProperties.PREFIX,
    name = ["enabled"],
    matchIfMissing = true
)
@EnableConfigurationProperties(LiteGatewayConfigProperties::class)
@AutoConfigureBefore(HttpHandlerAutoConfiguration::class, WebFluxAutoConfiguration::class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
class LiteGatewayAutoConfiguration