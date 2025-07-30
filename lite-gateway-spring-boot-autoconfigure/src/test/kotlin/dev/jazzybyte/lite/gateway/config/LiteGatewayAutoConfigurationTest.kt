package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.context.ServerWebExchangeRequestContext
import dev.jazzybyte.lite.gateway.route.PathPredicate
import dev.jazzybyte.lite.gateway.route.StaticRouteLocator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import kotlin.test.assertTrue

/**
 * [LiteGatewayAutoConfiguration]에 대한 테스트 클래스.
 *
 * `ApplicationContextRunner`를 사용하여 다양한 조건에서 자동 설정의 동작을 검증한다.
 */
class LiteGatewayAutoConfigurationTest {

    // 테스트를 위한 ReactiveWebApplicationContextRunner 를 통해 실행한다.
    // LiteGatewayAutoConfiguration을 테스트 대상 자동 설정으로 지정
    private val contextRunner = ReactiveWebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(LiteGatewayAutoConfiguration::class.java))

    @Test
    fun `auto configuration is enabled and ConfigProperties bean is registered when gateway property is missing`() {
        contextRunner.run { context ->
            // given: no specific property settings

            // when: context is loaded

            // then: LiteGatewayAutoConfiguration and ConfigProperties beans should exist.
            assertThat(context).hasSingleBean(LiteGatewayAutoConfiguration::class.java)
            assertThat(context).hasSingleBean(LiteGatewayConfigProperties::class.java)
        }
    }

    @Test
    fun `auto configuration is enabled when lite-gateway-enabled is true`() {
        contextRunner
            .withPropertyValues("lite.gateway.enabled=true") // given: enable property set to true
            .run { context ->
                // when: context is loaded

                // then: all related beans should exist.
                assertThat(context).hasSingleBean(LiteGatewayAutoConfiguration::class.java)
                assertThat(context).hasSingleBean(LiteGatewayConfigProperties::class.java)
            }
    }

    @Test
    fun `auto configuration is disabled when lite-gateway-enabled is false`() {
        contextRunner
            .withPropertyValues("lite.gateway.enabled=false") // given: enable property set to false
            .run { context ->
                // when: context is loaded

                // then: related beans should not exist.
                assertThat(context).doesNotHaveBean(LiteGatewayAutoConfiguration::class.java)
                assertThat(context).doesNotHaveBean(LiteGatewayConfigProperties::class.java)
            }
    }

    @Test
    fun `route properties are correctly bound to ConfigProperties object`() {
        // given: test route information set as properties
        contextRunner
            .withPropertyValues(
                "lite.gateway.routes[0].id=test-route",
                "lite.gateway.routes[0].uri=http://example.com",
                "lite.gateway.routes[0].predicates[0].name=Path",
                "lite.gateway.routes[0].predicates[0].args=/foo/**",
            )
            .run { context ->
                // when: context is loaded

                // then: verify that properties are correctly set in ConfigProperties bean.
                assertThat(context).hasSingleBean(LiteGatewayConfigProperties::class.java)
                val properties = context.getBean(LiteGatewayConfigProperties::class.java)

                assertThat(properties.routes).hasSize(1)
                val route = properties.routes[0]
                assertThat(route.id).isEqualTo("test-route")
                assertThat(route.uri).isEqualTo("http://example.com")


                // and: verify that StaticRouteLocator is correctly registered
                assertThat(context).hasSingleBean(StaticRouteLocator::class.java)
                val routeLocator = context.getBean(StaticRouteLocator::class.java)
                assertThat(routeLocator.routes).hasSize(1)
                val registeredRoute = routeLocator.routes.first()
                assertThat(registeredRoute.predicates).hasSize(1)
                assertTrue { registeredRoute.predicates[0] is PathPredicate }
                val pathPredicate = registeredRoute.predicates[0] as PathPredicate

                val serverWebExchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("http://test.com/foo/1")
                )
                assertTrue { pathPredicate.matches(ServerWebExchangeRequestContext(serverWebExchange)) }

            }
    }
}
