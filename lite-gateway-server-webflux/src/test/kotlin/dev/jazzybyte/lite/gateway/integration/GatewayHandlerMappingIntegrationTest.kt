package dev.jazzybyte.lite.gateway.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import dev.jazzybyte.lite.gateway.client.WebFluxHttpClient
import dev.jazzybyte.lite.gateway.handler.FilterHandler
import dev.jazzybyte.lite.gateway.handler.GatewayHandlerMapping
import dev.jazzybyte.lite.gateway.route.HostPredicate
import dev.jazzybyte.lite.gateway.route.StaticRouteLocator
import dev.jazzybyte.lite.gateway.route.PathPredicate
import dev.jazzybyte.lite.gateway.route.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.util.DefaultUriBuilderFactory

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [GatewayHandlerMappingIntegrationTest.TestConfig::class],
)
@DirtiesContext
class GatewayHandlerMappingIntegrationTest {

    // 로거
    private val log = KotlinLogging.logger {}

    @LocalServerPort
    val port: Int = 0

    /**
     * 웹 엔드포인트를 테스트용 클라이언트
     */
    lateinit var testClient: WebTestClient

    /**
     * 테스트용 기본 URI (scheme://host:port)
     */
    lateinit var baseUri: String

    companion object {
        private lateinit var wireMockServer: WireMockServer

        // BeforeAll
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
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

        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            wireMockServer.stop()
            wireMockServer.shutdown()
        }
    }

    @BeforeEach
    fun setup() {
        wireMockServer.resetAll()
        configureFor("localhost", wireMockServer.port())

        setup(ReactorClientHttpConnector(), "http://localhost:$port")

    }

    protected fun setup(httpConnector: ClientHttpConnector?, baseUri: String) {
        this.baseUri = baseUri
        val uriBuilderFactory = DefaultUriBuilderFactory(this.baseUri)
        this.testClient = WebTestClient.bindToServer(httpConnector!!)
            .uriBuilderFactory(uriBuilderFactory)
            .baseUrl(this.baseUri)
            .build()
    }

    @Test
    fun `should route GET to mock server`() {
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

    @Test
    fun `should route POST to mock server`() {
        // GIVEN
        stubFor(
            post(urlEqualTo("/api/users"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("POST users Match")
                )
        )

        // WHEN
        val responseSpec = testClient.post().uri("/api/users")
            .exchange()

        // THEN
        responseSpec
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("POST users Match")
        verify(postRequestedFor(urlEqualTo("/api/users")))
    }

    @Test
    fun `should route GET with query param to mock server`() {
        // GIVEN
        stubFor(
            get(urlPathEqualTo("/api/param"))
                .withQueryParam("param", matching(".*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("Query param: mock-value")
                )
        )

        // WHEN
        val responseSpec = testClient.get().uri { uriBuilder ->
            uriBuilder.path("/api/param").queryParam("param", "mock-value").build()
        }.exchange()

        // THEN
        responseSpec
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("Query param: mock-value")
        verify(
            getRequestedFor(urlPathEqualTo("/api/param"))
                .withQueryParam("param", equalTo("mock-value"))
        )
    }

    @EnableAutoConfiguration
    @SpringBootConfiguration
    class TestConfig {

        @Bean
        fun inMemoryRouteLocator(): StaticRouteLocator {
            return StaticRouteLocator(
                routes = listOf(
                    Route(
                        id = "test-route",
                        order = 0,
                        uri = wireMockServer.baseUrl(),
                        predicates = listOf(PathPredicate("/api/*"), HostPredicate("*"))
                    )
                )
            )
        }

        @Bean
        fun filterHandler() = FilterHandler(WebFluxHttpClient(), emptyList())

        @Bean
        fun gatewayHandlerMapping(
            staticRouteLocator: StaticRouteLocator,
            filterHandler: FilterHandler,
        ): GatewayHandlerMapping {
            return GatewayHandlerMapping(staticRouteLocator, filterHandler)
        }

    }


}