package dev.jazzybyte.lite.gateway.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.test.annotation.DirtiesContext

@SpringBootTest
@DirtiesContext
class FilterHandlerTest {

    @Autowired
    lateinit var filterHandler: FilterHandler

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Test
    fun `test filter handler initialization`() {
        // given
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users").build())

        // when
        val result = filterHandler.handle(exchange)
            .doOnSuccess { log.info { "Filter handler executed successfully." } }

        // then
        result.block()
    }


    @EnableAutoConfiguration
    @SpringBootConfiguration
    class TestConfig {
        @Bean
        fun filterHandler() = FilterHandler()
    }
}