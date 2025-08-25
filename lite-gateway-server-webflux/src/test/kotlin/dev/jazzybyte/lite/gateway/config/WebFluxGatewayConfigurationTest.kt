package dev.jazzybyte.lite.gateway.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner

class WebFluxGatewayConfigurationTest {


  // 테스트를 위한 ReactiveWebApplicationContextRunner 를 통해 실행한다.
  // LiteGatewayAutoConfiguration을 테스트 대상 자동 설정으로 지정
  private val contextRunner = ReactiveWebApplicationContextRunner()
   .withConfiguration(AutoConfigurations.of(LiteGatewayAutoConfiguration::class.java))
   .withConfiguration(AutoConfigurations.of(WebFluxGatewayConfiguration::class.java))

  @Test
  fun `auto configuration is enabled and ConfigProperties bean is registered when gateway property is missing`() {
   contextRunner.run { context ->
    // given: no specific property settings

    // when: context is loaded

    // then: WebFluxGatewayConfiguration beans should exist.
    assertThat(context).hasSingleBean(LiteGatewayAutoConfiguration::class.java)
    assertThat(context).hasSingleBean(LiteGatewayConfigProperties::class.java)
    assertThat(context).hasSingleBean(WebFluxGatewayConfiguration::class.java)
   }
  }
 }