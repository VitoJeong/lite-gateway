package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.exception.FilterExecutionException
import dev.jazzybyte.lite.gateway.filter.CriticalFilter
import dev.jazzybyte.lite.gateway.filter.FilterErrorResponse
import dev.jazzybyte.lite.gateway.filter.GatewayContext
import dev.jazzybyte.lite.gateway.filter.GatewayFilter
import dev.jazzybyte.lite.gateway.filter.GatewayFilterChain
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * 오류 처리 기능이 포함된 게이트웨이 필터 체인 구현체
 * 
 * 이 클래스는 필터 실행 중 발생하는 예외를 처리하고,
 * Critical/Non-Critical 필터를 구분하여 적절한 오류 처리를 수행한다.
 */
class ErrorHandlingGatewayFilterChain(
    private val filters: List<GatewayFilter>,
    private val finalAction: (GatewayContext) -> Mono<Void>,
    private val index: Int = 0,
    private val routeId: String? = null,
    private val requestId: String? = null
) : GatewayFilterChain {

    companion object {
        private val logger = LoggerFactory.getLogger(ErrorHandlingGatewayFilterChain::class.java)
    }

    override fun filter(context: GatewayContext): Mono<Void> {
        return Mono.defer {
            if (this.index < filters.size) {
                val currentFilter = filters[this.index]
                val filterName = currentFilter::class.simpleName ?: "UnknownFilter"
                
                // 다음 필터를 위해 새로운 체인 인스턴스 생성
                val nextChain = ErrorHandlingGatewayFilterChain(
                    filters, 
                    finalAction, 
                    this.index + 1,
                    routeId,
                    requestId
                )
                
                // 필터 실행 및 오류 처리
                currentFilter.filter(context, nextChain)
                    .doOnError { throwable ->
                        logFilterError(filterName, throwable)
                    }
                    .onErrorResume { throwable ->
                        handleFilterError(currentFilter, filterName, throwable, context)
                    }
            } else {
                // 모든 필터가 실행되었으므로 최종 액션 수행
                finalAction(context)
                    .doOnError { throwable ->
                        logger.error("Final action execution failed for route: {}, request: {}", 
                            routeId, requestId, throwable)
                    }
            }
        }
    }

    /**
     * 필터 오류를 로깅한다.
     */
    private fun logFilterError(filterName: String, throwable: Throwable) {
        when (throwable) {
            is FilterExecutionException -> {
                logger.error(
                    "Filter execution failed - Filter: {}, Route: {}, Request: {}, Error: {}",
                    filterName, throwable.routeId, throwable.requestId, throwable.message,
                    throwable
                )
            }
            else -> {
                logger.error(
                    "Unexpected error in filter - Filter: {}, Route: {}, Request: {}, Error: {}",
                    filterName, routeId, requestId, throwable.message,
                    throwable
                )
            }
        }
    }

    /**
     * 필터 오류를 처리한다.
     * Critical 필터의 경우 오류 응답을 반환하고,
     * Non-Critical 필터의 경우 다음 필터를 계속 실행한다.
     */
    private fun handleFilterError(
        filter: GatewayFilter,
        filterName: String,
        throwable: Throwable,
        context: GatewayContext
    ): Mono<Void> {
        return if (filter is CriticalFilter) {
            // Critical 필터 실패 시 오류 응답 반환
            handleCriticalFilterError(filter, filterName, throwable, context)
        } else {
            // Non-Critical 필터 실패 시 다음 필터 계속 실행
            handleNonCriticalFilterError(filterName, throwable, context)
        }
    }

    /**
     * Critical 필터 오류를 처리한다.
     * 적절한 HTTP 오류 응답을 생성하고 필터 체인을 중단한다.
     */
    private fun handleCriticalFilterError(
        criticalFilter: CriticalFilter,
        filterName: String,
        throwable: Throwable,
        context: GatewayContext
    ): Mono<Void> {
        val statusCode = criticalFilter.getFailureStatusCode()
        val errorMessage = criticalFilter.getFailureMessage(throwable)
        
        val errorResponse = when (throwable) {
            is FilterExecutionException -> {
                FilterErrorResponse.fromFilterExecutionException(
                    throwable,
                    statusCode,
                    errorMessage
                )
            }
            else -> {
                FilterErrorResponse.fromException(
                    filterName,
                    throwable,
                    statusCode,
                    errorMessage,
                    routeId,
                    requestId
                )
            }
        }
        
        logger.warn(
            "Critical filter failed, returning error response - Filter: {}, Status: {}, Route: {}, Request: {}",
            filterName, statusCode, routeId, requestId
        )
        
        return createErrorResponse(context, errorResponse)
    }

    /**
     * Non-Critical 필터 오류를 처리한다.
     * 오류를 로깅하고 다음 필터를 계속 실행한다.
     */
    private fun handleNonCriticalFilterError(
        filterName: String,
        throwable: Throwable,
        context: GatewayContext
    ): Mono<Void> {
        logger.warn(
            "Non-critical filter failed, continuing with next filter - Filter: {}, Route: {}, Request: {}, Error: {}",
            filterName, routeId, requestId, throwable.message
        )
        
        // 다음 필터를 위해 새로운 체인 인스턴스 생성하여 계속 실행
        val nextChain = ErrorHandlingGatewayFilterChain(
            filters,
            finalAction,
            this.index + 1,
            routeId,
            requestId
        )
        
        return nextChain.filter(context)
    }

    /**
     * 오류 응답을 생성한다.
     * 구체적인 구현은 WebFlux 통합 시 완성될 예정이다.
     */
    private fun createErrorResponse(context: GatewayContext, errorResponse: FilterErrorResponse): Mono<Void> {
        // TODO: WebFlux 통합 시 실제 HTTP 응답 생성 로직 구현
        // 현재는 로깅만 수행하고 빈 Mono 반환
        logger.debug("Creating error response: {}", errorResponse)
        return Mono.empty()
    }
}