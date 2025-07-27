package dev.jazzybyte.lite.gateway.context

import dev.jazzybyte.lite.gateway.http.GatewayHttpCookie
import dev.jazzybyte.lite.gateway.http.GatewayHttpMethod

interface RequestContext {
    fun host(): String
    fun path(): String
    fun method(): GatewayHttpMethod
    fun header(name: String): String?
    fun headers(name: String): List<String>
    fun query(name: String): String?
    fun cookies(name: String): List<GatewayHttpCookie>?
}