package dev.jazzybyte.lite.gateway.client

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.validation.annotation.Validated

@Validated
data class HttpClientProperties @ConstructorBinding constructor(
    @field:Min(value = 1, message = "maxConnections는 1 이상이어야 합니다.")
    val maxConnections: Int = 500,

    @field:Min(value = 0, message = "connectionTimeout은 0 이상이어야 합니다.")
    val connectionTimeout: Int = 5000,

    @field:Min(value = 1, message = "maxHeaderSize는 1 이상이어야 합니다.")
    val maxHeaderSize: Int = 8192,

    @field:Min(value = 0, message = "acquireTimeout은 0 이상이어야 합니다.")
    val acquireTimeout: Long = 10000
) {
    constructor() : this(
        maxConnections = 500,
        connectionTimeout = 5000,
        maxHeaderSize = 8192,
        acquireTimeout = 10000
    )
}