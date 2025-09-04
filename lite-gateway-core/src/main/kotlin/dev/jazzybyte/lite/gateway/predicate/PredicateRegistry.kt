package dev.jazzybyte.lite.gateway.predicate

import dev.jazzybyte.lite.gateway.exception.PredicateDiscoveryException
import dev.jazzybyte.lite.gateway.util.ReflectionUtil
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Predicate 클래스의 발견, 등록, 관리를 담당하는 클래스이다.
 * 패키지 스캔을 통해 RoutePredicate 구현체들을 찾고 이름-클래스 매핑을 관리한다.
 */
class PredicateRegistry {

    private val predicateClasses: Map<String, Class<out RoutePredicate>>

    init {
        predicateClasses = initializePredicateClasses()
    }

    /**
     * 등록된 Predicate 클래스들의 이름 목록을 반환한다.
     */
    fun getAvailablePredicateNames(): Set<String> = predicateClasses.keys

    /**
     * Predicate 이름으로 클래스를 조회한다.
     */
    fun getPredicateClass(predicateName: String): Class<out RoutePredicate>? {
        return predicateClasses[predicateName]
    }

    /**
     * Predicate가 등록되어 있는지 확인한다.
     */
    fun isPredicateRegistered(predicateName: String): Boolean {
        return predicateClasses.containsKey(predicateName)
    }

    /**
     * 등록된 Predicate 클래스 수를 반환한다.
     */
    fun getRegisteredPredicateCount(): Int = predicateClasses.size

    /**
     * Predicate 클래스들을 초기화하고 검증하는 함수
     * 패키지 스캔 결과를 검증하고 중복 Predicate 이름을 처리한다.
     */
    private fun initializePredicateClasses(): Map<String, Class<out RoutePredicate>> {
        val packageName = "dev.jazzybyte.lite.gateway.route"
        val discoveryStartTime = System.currentTimeMillis()
        
        try {
            log.info { "Starting predicate discovery in package: $packageName" }
            
            // 패키지 스캔을 통해 RoutePredicate 구현체들을 찾음
            val discoveredClasses = ReflectionUtil.findClassesOfType(packageName, RoutePredicate::class.java)
            
            // 패키지 스캔 결과 검증
            validatePackageScanResults(discoveredClasses, packageName)
            
            // Predicate 이름과 클래스 매핑 생성 및 중복 검증
            val predicateMap = buildPredicateMap(discoveredClasses)
            
            val discoveryTime = System.currentTimeMillis() - discoveryStartTime
            
            // Predicate 클래스 발견 시 이름과 클래스 매핑을 구조화된 형태로 로깅
            log.info {
                "Predicate discovery completed successfully: " +
                "discovered_classes=${discoveredClasses.size}, " +
                "registered_predicates=${predicateMap.size}, " +
                "discovery_time=${discoveryTime}ms"
            }

            // 디버그 레벨에서 추가 상세 정보 제공
            if (log.isDebugEnabled()) {
                // 각 Predicate 이름과 클래스 매핑의 상세 로깅
                predicateMap.forEach { (name, clazz) ->
                    log.debug { "Registered predicate mapping: name='$name' -> class='${clazz.name}'" }
                }

                log.debug { "Complete predicate class mappings: $predicateMap" }

                // 각 클래스의 생성자 정보도 로깅
                predicateMap.forEach { (name, clazz) ->
                    val constructors = clazz.constructors.map { constructor ->
                        val paramTypes = constructor.parameterTypes.joinToString(", ") { it.simpleName }
                        "($paramTypes)"
                    }
                    log.debug { "Predicate '$name' available constructors: ${constructors.joinToString(", ")}" }
                }
            }
            
            return predicateMap
            
        } catch (e: Exception) {
            val discoveryTime = System.currentTimeMillis() - discoveryStartTime
            val errorMessage = "Failed to discover predicate classes in package '$packageName' after ${discoveryTime}ms"
            
            log.error(e) { errorMessage }
            
            throw PredicateDiscoveryException(
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
        discoveredClasses: List<Class<out RoutePredicate>>,
        packageName: String
    ) {
        if (discoveredClasses.isEmpty()) {
            throw PredicateDiscoveryException(
                message = "No predicate classes found during package scan. " +
                        "Ensure that predicate classes exist and implement RoutePredicate interface.",
                packageName = packageName
            )
        }
        
        log.debug { "Found ${discoveredClasses.size} predicate classes: ${discoveredClasses.map { it.simpleName }}" }
        
        // 각 클래스가 올바르게 로드되었는지 검증
        discoveredClasses.forEach { predicateClass ->
            try {
                // 클래스 로딩 검증 - 클래스가 실제로 접근 가능한지 확인
                predicateClass.constructors
                predicateClass.simpleName
            } catch (e: Exception) {
                throw PredicateDiscoveryException(
                    message = "Failed to load predicate class '${predicateClass.name}'. " +
                            "Class may be corrupted or have accessibility issues.",
                    predicateName = predicateClass.simpleName,
                    packageName = packageName,
                    cause = e
                )
            }
        }
    }

    /**
     * Predicate 클래스들로부터 이름-클래스 매핑을 생성하고 중복을 처리한다.
     */
    private fun buildPredicateMap(
        discoveredClasses: List<Class<out RoutePredicate>>
    ): Map<String, Class<out RoutePredicate>> {
        val predicateMap = mutableMapOf<String, Class<out RoutePredicate>>()
        val duplicateNames = mutableSetOf<String>()
        
        discoveredClasses.forEach { predicateClass ->
            val predicateName = predicateClass.simpleName.removeSuffix("Predicate")
            
            // 빈 이름 검증
            if (predicateName.isBlank()) {
                throw PredicateDiscoveryException(
                    message = "Predicate class '${predicateClass.name}' has invalid name. " +
                            "Class name should follow the pattern '*Predicate' where * is not empty.",
                    predicateName = predicateClass.simpleName
                )
            }
            
            // 중복 이름 검증
            if (predicateMap.containsKey(predicateName)) {
                duplicateNames.add(predicateName)
                val existingClass = predicateMap[predicateName]!!
                
                log.warn { 
                    "Duplicate predicate name '$predicateName' found. " +
                    "Classes: '${existingClass.name}' and '${predicateClass.name}'. " +
                    "Using the first discovered class: '${existingClass.name}'"
                }
            } else {
                predicateMap[predicateName] = predicateClass
                log.debug { "Registered predicate: '$predicateName' -> '${predicateClass.name}'" }
            }
        }
        
        // 중복 이름이 발견된 경우 경고 로그 출력
        if (duplicateNames.isNotEmpty()) {
            log.warn { 
                "Found ${duplicateNames.size} duplicate predicate names: $duplicateNames. " +
                "Only the first discovered class for each name will be used. " +
                "Consider renaming predicate classes to avoid conflicts."
            }
        }
        
        return predicateMap.toMap()
    }
}