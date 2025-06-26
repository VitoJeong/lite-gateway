package dev.jazzybyte.lite.gateway.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import dev.jazzybyte.lite.gateway.handler.FilterHandler
import dev.jazzybyte.lite.gateway.handler.GatewayHandlerMapping
import dev.jazzybyte.lite.gateway.route.InMemoryRouteLocator
import dev.jazzybyte.lite.gateway.route.PathPredicate
import dev.jazzybyte.lite.gateway.route.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [GatewayHandlerMappingIntegrationTest.TestConfig::class],
)
@ContextConfiguration(initializers = [GatewayHandlerMappingIntegrationTest.RouteInitializer::class])
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
    fun setup() {
        configureFor("localhost", RouteInitializer.wireMockServer.port())

        setup(ReactorClientHttpConnector(), "http://localhost:$port")
    }

    @AfterEach
    fun teardown() {
        RouteInitializer.wireMockServer.stop()
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
        // GIVEN
        // Stub 설정
        stubFor(
            get(urlEqualTo("/api/users"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("GET users Match")
                )
        )

        // WHEN
        val responseSpec = testClient.get().uri("/api/users")
            .exchange()

        // THEN
        responseSpec
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("GET users Match")
        verify(getRequestedFor(urlEqualTo("/api/users")))
    }

    @EnableAutoConfiguration
    @SpringBootConfiguration
    class TestConfig {

        @Value("\${wiremock.port}")
        var mockPort: Int = 0

        @Bean
        fun inMemoryRouteLocator(): InMemoryRouteLocator {
            return InMemoryRouteLocator(
                routes = listOf(
                    Route(
                        id = "test-route",
                        order = 0,
                        uri = "http://localhost:$mockPort",
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

    class RouteInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

        companion object {
            lateinit var wireMockServer: WireMockServer
                private set
        }

        override fun initialize(context: ConfigurableApplicationContext) {
            wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
            wireMockServer.start()

            val requests = wireMockServer.allServeEvents
            requests.forEach {
                println("Received: ${it.request.method} ${it.request.url}")
            }
            val unmatched = wireMockServer.findUnmatchedRequests()
            unmatched.requests.forEach {
                println("Unmatched request: ${it.method} ${it.url}")
            }

            val env = context.environment as ConfigurableEnvironment
            val props = mapOf("wiremock.port" to wireMockServer.port().toString())
            val propertySource = MapPropertySource("wiremock", props)
            env.propertySources.addFirst(propertySource)
        }
    }


}