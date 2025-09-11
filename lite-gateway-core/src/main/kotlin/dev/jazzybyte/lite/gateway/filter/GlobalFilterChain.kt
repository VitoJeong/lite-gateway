package dev.jazzybyte.lite.gateway.filter

import reactor.core.publisher.Mono

/**
 * 전역 필터와 라우트별 필터를 결합하여 단일 실행 체인으로 관리하는 클래스.
 * 
 * 이 클래스는 다음과 같은 기능을 제공한다:
 * - 전역 필터(GlobalFilter)와 라우트별 필터(RouteFilter)의 통합
 * - 필터 순서(order) 기반 정렬
 * - 필터 타입별 일관된 실행 순서 보장
 * 
 * 필터 실행 순서:
 * 1. order 값에 따른 오름차순 정렬 (낮은 값이 먼저 실행)
 * 2. 동일한 order 값의 경우 타입별 우선순위 (Global > Route)
 * 3. 동일한 타입과 order의 경우 등록 순서 유지
 */
class GlobalFilterChain private constructor(
    private val sortedFilters: List<GatewayFilter>,
    private val finalAction: (GatewayContext) -> Mono<Void>
) : GatewayFilterChain {

    /**
     * 필터 체인을 실행한다.
     * 
     * @param context 현재 요청/응답 컨텍스트
     * @return 필터 체인의 완료를 나타내는 Mono<Void>
     */
    override fun filter(context: GatewayContext): Mono<Void> {
        return if (sortedFilters.isEmpty()) {
            finalAction(context)
        } else {
            // DefaultGatewayFilterChain을 사용하여 실제 필터 실행
            val chain = createExecutionChain(sortedFilters, finalAction)
            chain.filter(context)
        }
    }

    companion object {
        /**
         * GlobalFilterChain을 생성하는 빌더 클래스
         */
        class Builder {
            private val globalFilters = mutableListOf<GatewayFilter>()
            private val routeFilters = mutableListOf<GatewayFilter>()
            private var finalAction: ((GatewayContext) -> Mono<Void>)? = null

            /**
             * 전역 필터를 추가한다.
             * 
             * @param filter 추가할 전역 필터
             * @return 빌더 인스턴스
             */
            fun addGlobalFilter(filter: GatewayFilter): Builder {
                globalFilters.add(filter)
                return this
            }

            /**
             * 전역 필터 목록을 추가한다.
             * 
             * @param filters 추가할 전역 필터 목록
             * @return 빌더 인스턴스
             */
            fun addGlobalFilters(filters: List<GatewayFilter>): Builder {
                globalFilters.addAll(filters)
                return this
            }

            /**
             * 라우트별 필터를 추가한다.
             * 
             * @param filter 추가할 라우트별 필터
             * @return 빌더 인스턴스
             */
            fun addRouteFilter(filter: GatewayFilter): Builder {
                routeFilters.add(filter)
                return this
            }

            /**
             * 라우트별 필터 목록을 추가한다.
             * 
             * @param filters 추가할 라우트별 필터 목록
             * @return 빌더 인스턴스
             */
            fun addRouteFilters(filters: List<GatewayFilter>): Builder {
                routeFilters.addAll(filters)
                return this
            }

            /**
             * 최종 액션을 설정한다.
             * 
             * @param action 모든 필터 실행 후 수행될 최종 액션
             * @return 빌더 인스턴스
             */
            fun finalAction(action: (GatewayContext) -> Mono<Void>): Builder {
                this.finalAction = action
                return this
            }

            /**
             * GlobalFilterChain을 빌드한다.
             * 
             * @return 구성된 GlobalFilterChain 인스턴스
             * @throws IllegalStateException finalAction이 설정되지 않은 경우
             */
            fun build(): GlobalFilterChain {
                val action = finalAction ?: throw IllegalStateException("Final action must be set")
                
                // 모든 필터를 결합하고 정렬
                val allFilters = mutableListOf<FilterWithType>()
                
                // 전역 필터 추가 (타입 우선순위: 0)
                globalFilters.forEach { filter ->
                    allFilters.add(FilterWithType(filter, FilterType.GLOBAL, 0))
                }
                
                // 라우트별 필터 추가 (타입 우선순위: 1)
                routeFilters.forEach { filter ->
                    allFilters.add(FilterWithType(filter, FilterType.ROUTE, 1))
                }
                
                // 필터 정렬
                val sortedFilters = sortFilters(allFilters)
                
                return GlobalFilterChain(sortedFilters, action)
            }
        }

        /**
         * 새로운 빌더 인스턴스를 생성한다.
         * 
         * @return 새로운 Builder 인스턴스
         */
        fun builder(): Builder = Builder()

        /**
         * 필터 목록을 정렬한다.
         * 
         * 정렬 기준:
         * 1. order 값 (오름차순)
         * 2. 필터 타입 우선순위 (Global > Route)
         * 3. 등록 순서 유지
         */
        private fun sortFilters(filters: List<FilterWithType>): List<GatewayFilter> {
            return filters
                .sortedWith(compareBy<FilterWithType> { getFilterOrder(it.filter) }
                    .thenBy { it.typePriority }
                    .thenBy { it.registrationOrder })
                .map { it.filter }
        }

        /**
         * 필터의 order 값을 반환한다.
         * OrderedGatewayFilter인 경우 getOrder() 값을, 그렇지 않으면 기본값 0을 반환한다.
         */
        private fun getFilterOrder(filter: GatewayFilter): Int {
            return if (filter is OrderedGatewayFilter) {
                filter.order
            } else {
                0 // 기본 order 값
            }
        }

        /**
         * 실제 필터 실행을 위한 체인을 생성한다.
         * 현재는 DefaultGatewayFilterChain을 사용하지만, 향후 다른 구현체로 변경 가능하다.
         */
        private fun createExecutionChain(
            filters: List<GatewayFilter>,
            finalAction: (GatewayContext) -> Mono<Void>
        ): GatewayFilterChain {
            // 여기서는 구체적인 구현체를 직접 참조하지 않고 팩토리 패턴을 사용할 수 있지만,
            // 현재는 단순화를 위해 직접 생성
            return object : GatewayFilterChain {
                override fun filter(context: GatewayContext): Mono<Void> {
                    return executeFilters(filters, 0, context, finalAction)
                }
            }
        }

        /**
         * 필터를 순차적으로 실행한다.
         * 재귀적 방식으로 각 필터를 실행하고 다음 필터로 체인을 전달한다.
         */
        private fun executeFilters(
            filters: List<GatewayFilter>,
            index: Int,
            context: GatewayContext,
            finalAction: (GatewayContext) -> Mono<Void>
        ): Mono<Void> {
            return if (index >= filters.size) {
                finalAction(context)
            } else {
                val currentFilter = filters[index]
                val nextChain = object : GatewayFilterChain {
                    override fun filter(context: GatewayContext): Mono<Void> {
                        return executeFilters(filters, index + 1, context, finalAction)
                    }
                }
                currentFilter.filter(context, nextChain)
            }
        }
    }

    /**
     * 필터 타입을 나타내는 열거형
     */
    private enum class FilterType {
        GLOBAL, ROUTE
    }

    /**
     * 필터와 관련 메타데이터를 포함하는 데이터 클래스
     */
    private data class FilterWithType(
        val filter: GatewayFilter,
        val type: FilterType,
        val typePriority: Int,
        val registrationOrder: Int = System.nanoTime().toInt() // 등록 순서 추적용
    )
}