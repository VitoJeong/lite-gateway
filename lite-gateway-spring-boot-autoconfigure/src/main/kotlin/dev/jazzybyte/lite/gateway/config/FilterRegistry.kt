package dev.jazzybyte.lite.gateway.config

import dev.jazzybyte.lite.gateway.exception.FilterDiscoveryException
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.util.ReflectionUtil
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * GatewayFilter 클래스의 발견, 등록, 관리를 담당하는 클래스이다.
 * 패키지 스캔을 통해 GatewayFilter 구현체들을 찾고 이름-클래스 매핑을 관리한다.
 * 모든 메서드는 정적으로 제공된다.
 */
class FilterRegistry {

    companion object {
        private val log = KotlinLogging.logger {}

        private val filterClasses: Map<String, Class<out GatewayFilter>>

        init {
            filterClasses = initializeFilterClasses()
        }

        /**
         * 등록된 Filter 클래스들의 이름 목록을 반환한다.
         */
        @JvmStatic
        fun getAvailableFilterNames(): Set<String> = filterClasses.keys

        /**
         * Filter 이름으로 클래스를 조회한다.
         */
        @JvmStatic
        fun getFilterClass(filterName: String): Class<out GatewayFilter> {
            return filterClasses[filterName] ?: throw FilterDiscoveryException(
                message = "Filter with name '$filterName' is not registered.",
                filterName = filterName
            )
        }

        /**
         * Filter가 등록되어 있는지 확인한다.
         */
        @JvmStatic
        fun isFilterRegistered(filterName: String): Boolean {
            return filterClasses.containsKey(filterName)
        }

        /**
         * 등록된 Filter 클래스 수를 반환한다.
         */
        @JvmStatic
        fun getRegisteredFilterCount(): Int = filterClasses.size

        /**
         * Filter 클래스들을 초기화하고 검증하는 함수
         * 패키지 스캔 결과를 검증하고 중복 Filter 이름을 처리한다.
         */
        private fun initializeFilterClasses(): Map<String, Class<out GatewayFilter>> {
            val packageName = "dev.jazzybyte.lite.gateway.filter"
            val discoveryStartTime = System.currentTimeMillis()

            try {
                val discoveredClasses = ReflectionUtil.findClassesOfType(packageName, GatewayFilter::class.java)

                validatePackageScanResults(discoveredClasses, packageName)

                val filterMap = buildFilterMap(discoveredClasses)

                val discoveryTime = System.currentTimeMillis() - discoveryStartTime

                log.info {
                    "Filter discovery completed successfully: " +
                            "discovered_classes=${discoveredClasses.size}, " +
                            "registered_filters=${filterMap.size}, " +
                            "discovery_time=${discoveryTime}ms"
                }

                if (log.isDebugEnabled()) {
                    filterMap.forEach { (name, clazz) ->
                        log.debug { "Registered filter mapping: name='$name' -> class='${clazz.name}'" }
                    }
                    log.debug { "Complete filter class mappings: $filterMap" }
                }

                return filterMap

            } catch (e: Exception) {
                val discoveryTime = System.currentTimeMillis() - discoveryStartTime
                val errorMessage =
                    "Failed to discover filter classes in package '$packageName' after ${discoveryTime}ms"

                log.error(e) { errorMessage }

                throw FilterDiscoveryException(
                    message = errorMessage,
                    packageName = packageName,
                    cause = e
                )
            }
        }

        /**
         * 패키지 스캔 결과를 검증한다.
         */
        private fun validatePackageScanResults(
            discoveredClasses: List<Class<out GatewayFilter>>,
            packageName: String,
        ) {
            if (discoveredClasses.isEmpty()) {
                throw FilterDiscoveryException(
                    message = "No filter classes found during package scan. " +
                            "Ensure that filter classes exist and implement GatewayFilter interface.",
                    packageName = packageName
                )
            }

            log.debug { "Found ${discoveredClasses.size} filter classes: ${discoveredClasses.map { it.simpleName }}" }

            discoveredClasses.forEach { filterClass ->
                try {
                    filterClass.constructors
                    filterClass.simpleName
                } catch (e: Exception) {
                    throw FilterDiscoveryException(
                        message = "Failed to load filter class '${filterClass.name}'. " +
                                "Class may be corrupted or have accessibility issues.",
                        filterName = filterClass.simpleName,
                        packageName = packageName,
                        cause = e
                    )
                }
            }
        }

        /**
         * Filter 클래스들로부터 이름-클래스 매핑을 생성하고 중복을 처리한다.
         */
        private fun buildFilterMap(
            discoveredClasses: List<Class<out GatewayFilter>>,
        ): Map<String, Class<out GatewayFilter>> {
            val filterMap = mutableMapOf<String, Class<out GatewayFilter>>()
            val duplicateNames = mutableSetOf<String>()

            discoveredClasses.forEach { filterClass ->
                val filterName = filterClass.simpleName.removeSuffix("Filter")

                if (filterName.isBlank()) {
                    throw FilterDiscoveryException(
                        message = "Filter class '${filterClass.name}' has invalid name. " +
                                "Class name should follow the pattern '*GatewayFilter' where * is not empty.",
                        filterName = filterClass.simpleName
                    )
                }

                if (filterMap.containsKey(filterName)) {
                    duplicateNames.add(filterName)
                    val existingClass = filterMap[filterName]!!

                    log.warn {
                        "Duplicate filter name '$filterName' found. " +
                                "Classes: '${existingClass.name}' and '${filterClass.name}'. " +
                                "Using the first discovered class: '${existingClass.name}'"
                    }
                } else {
                    filterMap[filterName] = filterClass
                    log.debug { "Registered filter: '$filterName' -> '${filterClass.name}'" }
                }
            }

            if (duplicateNames.isNotEmpty()) {
                log.warn {
                    "Found ${duplicateNames.size} duplicate filter names: $duplicateNames. " +
                            "Only the first discovered class for each name will be used. " +
                            "Consider renaming filter classes to avoid conflicts."
                }
            }

            return filterMap.toMap()
        }
    }
}
