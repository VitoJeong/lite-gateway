package dev.jazzybyte.lite.gateway.client

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