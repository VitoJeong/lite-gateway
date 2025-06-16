package dev.jazzybyte.lite.gateway.route

import org.springframework.web.server.ServerWebExchange

interface RouteLocator {
    fun getRoutes(exchange: ServerWebExchange): List<Route>
}