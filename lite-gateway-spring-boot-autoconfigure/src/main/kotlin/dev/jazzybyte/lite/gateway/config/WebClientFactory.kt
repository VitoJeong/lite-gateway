package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.client.HttpClientProperties
import dev.jazzybyte.lite.gateway.client.WebFluxHttpClient

class WebClientFactory {
    companion object {
        fun create(properties: HttpClientProperties): WebFluxHttpClient {
            return WebFluxHttpClient(properties.maxConnections,
                properties.connectionTimeout,
                properties.maxHeaderSize,
                properties.acquireTimeout)
        }
    }
}