package dev.jazzybyte.lite.gateway.context

import dev.jazzybyte.lite.gateway.http.GatewayHttpCookie
import dev.jazzybyte.lite.gateway.http.GatewayHttpMethod
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange

class ServerWebExchangeRequestContext (
    private val webExchange: ServerWebExchange
) : RequestContext {

    override fun path(): String {
        return webExchange.request.path.value()
    }

    override fun host(): String {
        return webExchange.request.uri.host
    }

    override fun method(): GatewayHttpMethod {
        return when (webExchange.request.method) {
            HttpMethod.GET -> GatewayHttpMethod.GET
            HttpMethod.POST -> GatewayHttpMethod.POST
            HttpMethod.PUT -> GatewayHttpMethod.PUT
            HttpMethod.DELETE -> GatewayHttpMethod.DELETE
            HttpMethod.PATCH -> GatewayHttpMethod.PATCH
            HttpMethod.OPTIONS -> GatewayHttpMethod.OPTIONS
            HttpMethod.HEAD -> GatewayHttpMethod.HEAD
            HttpMethod.TRACE -> GatewayHttpMethod.TRACE

            else -> throw IllegalArgumentException("Unsupported method ${webExchange.request.method}")
        }
    }

    override fun header(name: String): String? {
        return webExchange.request.headers.getFirst(name)
    }

    override fun headers(name: String): List<String> {
        return webExchange.request.headers[name] ?: emptyList()
    }

    override fun query(name: String): String? {
        return webExchange.request.queryParams.getFirst(name)
    }

    override fun cookies(name: String): List<GatewayHttpCookie>? {
        return webExchange.request.cookies[name]?.map {
            GatewayHttpCookie(it.name, it.value)
        }
    }
}