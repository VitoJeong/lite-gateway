package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.config.FilterRegistry
import dev.jazzybyte.lite.gateway.exception.FilterInstantiationException
import dev.jazzybyte.lite.gateway.filter.AddRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.AddResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.CircuitBreakerGatewayFilter
import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.GatewayFilterFactory
import dev.jazzybyte.lite.gateway.filter.ModifyRequestBodyGatewayFilter
import dev.jazzybyte.lite.gateway.filter.ModifyResponseBodyGatewayFilter
import dev.jazzybyte.lite.gateway.filter.RemoveRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.RemoveResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.RequestRateLimiterGatewayFilter
import dev.jazzybyte.lite.gateway.filter.RewritePathGatewayFilter
import dev.jazzybyte.lite.gateway.filter.StripPrefixGatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType

class WebfluxGatewayFilterFactory : GatewayFilterFactory {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun create(definition: FilterDefinition): GatewayFilter {
        return try {
            validateFilterDefinition(definition)
            createFilterInstance(definition)
        } catch (e: FilterInstantiationException) {
            // Re-throw FilterInstantiationException as-is
            throw e
        } catch (e: Exception) {
            // Wrap other exceptions in FilterInstantiationException
            log.error(e) { "Unexpected error while creating filter '${definition.type}'" }
            throw FilterInstantiationException(
                message = "Unexpected error during filter creation: ${e.message}",
                filterType = definition.type,
                args = definition.args,
                cause = e
            )
        }
    }

    private fun validateFilterDefinition(definition: FilterDefinition) {
        // Validate filter type is not blank
        if (definition.type.isBlank()) {
            throw FilterInstantiationException(
                message = "Filter type cannot be blank",
                filterType = definition.type,
                args = definition.args
            )
        }

        // Check if filter is registered
        if (!FilterRegistry.isFilterRegistered(definition.type)) {
            val availableFilters = FilterRegistry.getAvailableFilterNames()
            throw FilterInstantiationException(
                message = "Filter type '${definition.type}' is not registered. Available filters: $availableFilters",
                filterType = definition.type,
                args = definition.args
            )
        }
    }

    private fun createFilterInstance(definition: FilterDefinition): GatewayFilter {
        val filterClass = FilterRegistry.getFilterClass(definition.type)
        val args = definition.args

        return try {
            when (filterClass) {
                AddRequestHeaderGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name", "value"))
                    AddRequestHeaderGatewayFilter(
                        name = args["name"]!!,
                        value = args["value"]!!
                    )
                }

                RemoveRequestHeaderGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name"))
                    RemoveRequestHeaderGatewayFilter(
                        name = args["name"]!!
                    )
                }

                AddResponseHeaderGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name", "value"))
                    AddResponseHeaderGatewayFilter(
                        name = args["name"]!!,
                        value = args["value"]!!
                    )
                }

                RemoveResponseHeaderGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name"))
                    RemoveResponseHeaderGatewayFilter(
                        name = args["name"]!!
                    )
                }

                RewritePathGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("regexp", "replacement"))
                    RewritePathGatewayFilter(
                        regexp = args["regexp"]!!,
                        replacement = args["replacement"]!!
                    )
                }

                StripPrefixGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("parts"))
                    val parts = args["parts"]!!.toIntOrNull() 
                        ?: throw FilterInstantiationException(
                            message = "Invalid parts value: ${args["parts"]}. Must be a positive integer.",
                            filterType = definition.type,
                            args = definition.args
                        )
                    StripPrefixGatewayFilter(parts = parts)
                }

                RequestRateLimiterGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("replenishRate", "burstCapacity"))
                    val replenishRate = args["replenishRate"]!!.toDoubleOrNull()
                        ?: throw FilterInstantiationException(
                            message = "Invalid replenishRate value: ${args["replenishRate"]}. Must be a positive number.",
                            filterType = definition.type,
                            args = definition.args
                        )
                    val burstCapacity = args["burstCapacity"]!!.toLongOrNull()
                        ?: throw FilterInstantiationException(
                            message = "Invalid burstCapacity value: ${args["burstCapacity"]}. Must be a positive integer.",
                            filterType = definition.type,
                            args = definition.args
                        )
                    val requestedTokens = args["requestedTokens"]?.toLongOrNull() ?: 1L
                    RequestRateLimiterGatewayFilter(
                        replenishRate = replenishRate,
                        burstCapacity = burstCapacity,
                        requestedTokens = requestedTokens
                    )
                }

                CircuitBreakerGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name"))
                    val failureRateThreshold = args["failureRateThreshold"]?.toFloatOrNull() ?: 50.0f
                    val waitDurationSeconds = args["waitDurationInOpenState"]?.toLongOrNull() ?: 60L
                    val slidingWindowSize = args["slidingWindowSize"]?.toIntOrNull() ?: 100
                    val fallbackResponse = args["fallbackResponse"] ?: "Service Unavailable"
                    
                    CircuitBreakerGatewayFilter(
                        name = args["name"]!!,
                        failureRateThreshold = failureRateThreshold,
                        waitDurationInOpenState = java.time.Duration.ofSeconds(waitDurationSeconds),
                        slidingWindowSize = slidingWindowSize,
                        fallbackResponse = fallbackResponse
                    )
                }

                ModifyRequestBodyGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("transformType"))
                    createModifyRequestBodyFilter(args, definition.order)
                }

                ModifyResponseBodyGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("transformType"))
                    createModifyResponseBodyFilter(args, definition.order)
                }

                else -> throw FilterInstantiationException(
                    message = "No factory method available for filter type '${definition.type}'",
                    filterType = definition.type,
                    args = definition.args
                )
            }
        } catch (e: FilterInstantiationException) {
            throw e
        } catch (e: Exception) {
            throw FilterInstantiationException(
                message = "Failed to instantiate filter: ${e.message}",
                filterType = definition.type,
                args = definition.args,
                cause = e
            )
        }
    }

    private fun validateRequiredArgs(definition: FilterDefinition, requiredArgs: List<String>) {
        // First check for completely missing arguments
        val missingArgs = requiredArgs.filter { arg ->
            !definition.args.containsKey(arg) || definition.args[arg] == null
        }

        if (missingArgs.isNotEmpty()) {
            throw FilterInstantiationException(
                message = "Missing required arguments: $missingArgs. Required arguments for '${definition.type}': $requiredArgs",
                filterType = definition.type,
                args = definition.args
            )
        }

        // Then validate argument values for blank strings
        requiredArgs.forEach { arg ->
            val value = definition.args[arg]
            if (value != null && value.isBlank()) {
                throw FilterInstantiationException(
                    message = "Argument '$arg' cannot be blank",
                    filterType = definition.type,
                    args = definition.args
                )
            }
        }
    }

    /**
     * ModifyRequestBodyGatewayFilter 인스턴스를 생성한다.
     */
    private fun createModifyRequestBodyFilter(args: Map<String, String>, order: Int): ModifyRequestBodyGatewayFilter {
        val transformType = args["transformType"]!!
        val contentType = args["contentType"]?.let { MediaType.parseMediaType(it) }
        
        val transformFunction = createTransformFunction(transformType, args)
        
        return ModifyRequestBodyGatewayFilter(
            transformFunction = transformFunction,
            contentType = contentType
        )
    }

    /**
     * ModifyResponseBodyGatewayFilter 인스턴스를 생성한다.
     */
    private fun createModifyResponseBodyFilter(args: Map<String, String>, order: Int): ModifyResponseBodyGatewayFilter {
        val transformType = args["transformType"]!!
        val contentType = args["contentType"]?.let { MediaType.parseMediaType(it) }
        val filterOrder = args["order"]?.toIntOrNull() ?: order
        
        val transformFunction = createTransformFunction(transformType, args)
        
        return ModifyResponseBodyGatewayFilter(
            transformFunction = transformFunction,
            contentType = contentType,
            order = filterOrder
        )
    }

    /**
     * 변환 함수를 생성한다.
     */
    private fun createTransformFunction(transformType: String, args: Map<String, String>): (String) -> String {
        return when (transformType.lowercase()) {
            "mask" -> {
                val maskPattern = args["maskPattern"] ?: "***"
                createMaskingFunction(maskPattern)
            }
            "uppercase" -> { body -> body.uppercase() }
            "lowercase" -> { body -> body.lowercase() }
            "remove_sensitive" -> createSensitiveDataRemovalFunction()
            "json_transform" -> createJsonTransformFunction(args)
            else -> throw FilterInstantiationException(
                message = "Unsupported transformType: $transformType. Supported types: mask, uppercase, lowercase, remove_sensitive, json_transform",
                filterType = "ModifyRequestBody/ModifyResponseBody",
                args = args
            )
        }
    }

    /**
     * 민감한 데이터를 마스킹하는 함수 생성
     */
    private fun createMaskingFunction(maskPattern: String): (String) -> String {
        return { body ->
            body
                // 이메일 마스킹
                .replace(Regex("""[\w._%+-]+@[\w.-]+\.[A-Za-z]{2,}""")) { matchResult ->
                    val email = matchResult.value
                    val atIndex = email.indexOf('@')
                    if (atIndex > 2) {
                        email.substring(0, 2) + maskPattern + email.substring(atIndex)
                    } else {
                        maskPattern + email.substring(atIndex)
                    }
                }
                // 전화번호 마스킹
                .replace(Regex("""\b\d{3}[-\s]?\d{4}[-\s]?\d{4}\b""")) { matchResult ->
                    val phone = matchResult.value
                    phone.substring(0, 3) + maskPattern + phone.substring(phone.length - 4)
                }
        }
    }

    /**
     * 민감한 데이터 제거 함수 생성
     */
    private fun createSensitiveDataRemovalFunction(): (String) -> String {
        return { body ->
            body
                // 신용카드 번호 패턴 제거
                .replace(Regex("""\b\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}\b"""), "[CARD_REMOVED]")
                // 주민등록번호 패턴 제거
                .replace(Regex("""\b\d{6}[-\s]?\d{7}\b"""), "[SSN_REMOVED]")
                // 전화번호 패턴 제거
                .replace(Regex("""\b\d{3}[-\s]?\d{4}[-\s]?\d{4}\b"""), "[PHONE_REMOVED]")
        }
    }

    /**
     * JSON 변환 함수 생성
     */
    private fun createJsonTransformFunction(args: Map<String, String>): (String) -> String {
        val removeFields = args["removeFields"]?.split(",")?.map { it.trim() } ?: emptyList()
        
        return { body ->
            if (removeFields.isNotEmpty()) {
                var transformedBody = body
                removeFields.forEach { field ->
                    // JSON 필드 제거 (간단한 정규식 기반, 실제 환경에서는 JSON 라이브러리 사용 권장)
                    transformedBody = transformedBody.replace(
                        Regex(""""$field"\s*:\s*[^,}]+[,}]"""), 
                        ""
                    )
                }
                transformedBody
            } else {
                body
            }
        }
    }
}