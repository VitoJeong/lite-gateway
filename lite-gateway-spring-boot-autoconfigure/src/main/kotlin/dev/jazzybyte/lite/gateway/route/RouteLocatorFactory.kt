package dev.jazzybyte.lite.gateway.route

/**
 * RouteLocator 생성 팩토리 인터페이스
 * 다양한 방식으로 RouteLocator를 생성할 수 있도록 확장성을 제공합니다.
 */
interface RouteLocatorFactory {
    fun create(routesDefinitions: MutableList<RouteDefinition>): RouteLocator
}