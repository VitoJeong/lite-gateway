package dev.jazzybyte.lite.gateway.client

import org.springframework.validation.annotation.Validated

@Validated
class HttpClientProperties (
    val maxConnections: Int = 500,
    val connectionTimeout: Int = 30 * 1000,
    val maxHeaderSize: Int = 30 * 1000,
    val acquireTimeout: Int = 20 * 1000,
) {

}
