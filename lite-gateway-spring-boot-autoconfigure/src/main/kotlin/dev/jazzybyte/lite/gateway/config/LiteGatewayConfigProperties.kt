package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.client.HttpClientProperties
import dev.jazzybyte.lite.gateway.config.LiteGatewayConfigProperties.Companion.PREFIX
import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.route.RouteDefinition
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = PREFIX)
data class LiteGatewayConfigProperties (

    /**
     * 게이트웨이에 적용할 라우트 목록.
     */
    @field:NotNull
    @field:Valid
    val routes: MutableList<RouteDefinition> = mutableListOf(),

    /**
     * 모든 라우트에 적용되는 필터 정의 목록.
     */
    @field:Valid
    val globalFilters: MutableList<FilterDefinition> = mutableListOf(),

    /**
     * HTTP 클라이언트 설정.
     */
    @field:Valid
    val httpClient: HttpClientProperties = HttpClientProperties()

) {
    companion object {
        const val PREFIX = "lite.gateway"
    }

}