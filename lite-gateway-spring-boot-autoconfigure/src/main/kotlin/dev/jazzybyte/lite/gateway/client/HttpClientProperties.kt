package dev.jazzybyte.lite.gateway.client

import org.springframework.validation.annotation.Validated

@Validated
class HttpClientProperties (
    val maxConnections: Int = 500,
    val connectionTimeout: Int = 5 * 1000,
    val maxHeaderSize: Int = 8192,
    val acquireTimeout: Long = 10 * 1000
) {

}
