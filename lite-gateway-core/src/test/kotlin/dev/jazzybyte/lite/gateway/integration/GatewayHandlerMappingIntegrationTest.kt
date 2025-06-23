package dev.jazzybyte.lite.gateway.integration

import dev.jazzybyte.lite.gateway.handler.FilterHandler
import dev.jazzybyte.lite.gateway.handler.GatewayHandlerMapping
import dev.jazzybyte.lite.gateway.route.InMemoryRouteLocator
import dev.jazzybyte.lite.gateway.route.PathPredicate
import dev.jazzybyte.lite.gateway.route.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [GatewayHandlerMappingIntegrationTest.TestConfig::class, TestController::class]
)
@DirtiesContext
class GatewayHandlerMappingIntegrationTest {

    // 로거
    private val log = KotlinLogging.logger {}

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @LocalServerPort
    val port: Int = 0

    lateinit var testClient: WebTestClient

    lateinit var webClient: WebClient

    lateinit var baseUri: String

    @BeforeEach
    @Throws(Exception::class)
    fun setup() {
        log.info { "Setting up WebTestClient for port: $port" }
        setup(ReactorClientHttpConnector(), "http://localhost:$port")
    }

    @Test
    fun `print all registered beans`() {
        val beanNames = applicationContext.beanDefinitionNames.sorted()
        println("Registered Beans in Spring Context:")
        beanNames.forEach { println(it) }
    }

    protected fun setup(httpConnector: ClientHttpConnector?, baseUri: String) {
        this.baseUri = baseUri
        this.webClient = WebClient.builder().clientConnector(httpConnector!!).baseUrl(this.baseUri).build()
        val uriBuilderFactory = DefaultUriBuilderFactory(this.baseUri)
        this.testClient = WebTestClient.bindToServer(httpConnector)
            .uriBuilderFactory(uriBuilderFactory)
            .baseUrl(this.baseUri)
            .build()
    }

    @Test
    fun `should route to correct handler`() {
        // given: 테스트 요청 URI
        val testUri = "/api/users"

        // when: WebTestClient를 사용하여 요청을 보냄
        val responseSpec = testClient.get()
            .uri(testUri)
            .exchange()


        responseSpec
            .expectStatus().is2xxSuccessful

    }

    @Test
    fun `verify handler mapping registration`() {
        val handlerMappings = applicationContext.getBeansOfType(HandlerMapping::class.java)
        println("Registered HandlerMappings:")
        handlerMappings.forEach { (name, mapping) ->
            println("$name: ${mapping::class.simpleName} (order: ${(mapping as? Ordered)?.order})")
        }

        assertTrue { handlerMappings.containsValue(applicationContext.getBean(GatewayHandlerMapping::class.java)) }
    }

    @Test
    fun `verify dispatcher handler`() {
        val dispatcherHandler = applicationContext.getBean(DispatcherHandler::class.java)
        val handlerMappings = dispatcherHandler.handlerMappings

        println("DispatcherHandler HandlerMappings:")
        handlerMappings?.forEach { mapping ->
            println("${mapping::class.simpleName} (order: ${(mapping as? Ordered)?.order})")
        }
    }


    @EnableAutoConfiguration
    @SpringBootConfiguration
    class TestConfig {

        @Bean
        fun inMemoryRouteLocator(): InMemoryRouteLocator {
            return InMemoryRouteLocator(
                routes = listOf(
                    Route(
                        id = "test-route",
                        order = 0,
                        uri = "http://localhost",
                        predicates = listOf(PathPredicate("/api/users"))
                    )
                )
            )
        }

        @Bean
        fun filterHandler() = FilterHandler()

        @Bean
        fun gatewayHandlerMapping(
            inMemoryRouteLocator: InMemoryRouteLocator,
            filterHandler: FilterHandler,
        ): GatewayHandlerMapping {
            return GatewayHandlerMapping(inMemoryRouteLocator, filterHandler)
        }

    }

}