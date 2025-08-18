package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.client.HttpClientProperties
import dev.jazzybyte.lite.gateway.client.WebFluxHttpClient

class WebClientFactory {
    companion object {
        fun create(httpClient: HttpClientProperties): WebFluxHttpClient {
            return WebFluxHttpClient()
        }
    }
}