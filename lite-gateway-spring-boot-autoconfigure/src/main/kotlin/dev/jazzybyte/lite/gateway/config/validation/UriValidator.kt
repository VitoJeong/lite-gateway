package dev.jazzybyte.lite.gateway.config.validation

import dev.jazzybyte.lite.gateway.exception.RouteConfigurationException
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * URI 검증을 담당하는 클래스입니다.
 * 라우트 정의에서 사용되는 URI의 형식과 유효성을 검증합니다.
 */
class UriValidator {

    /**
     * URI 형식을 강화된 검증 로직으로 검증합니다.
     */
    fun validateUriFormat(uriString: String, routeId: String) {
        try {
            // 1. 기본 스키마 검증
            if (!uriString.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) {
                throw RouteConfigurationException(
                    message = "Invalid URI format: '$uriString'. URI must have a valid scheme (e.g., http://, https://)",
                    routeId = routeId
                )
            }

            // 2. Java URI 파싱 검증
            val uri = java.net.URI(uriString)
            
            // 3. 스키마 존재 검증
            if (uri.scheme.isNullOrBlank()) {
                throw RouteConfigurationException(
                    message = "Invalid URI format: '$uriString'. URI scheme cannot be empty",
                    routeId = routeId
                )
            }

            // 4. 호스트 검증 (스키마가 네트워크 기반인 경우)
            if (isNetworkScheme(uri.scheme)) {
                validateNetworkHost(uri, uriString, routeId)
            }

            // 5. 포트 범위 검증
            if (uri.port != -1 && (uri.port < 1 || uri.port > 65535)) {
                throw RouteConfigurationException(
                    message = "Invalid URI format: '$uriString'. Port must be between 1 and 65535, but was ${uri.port}",
                    routeId = routeId
                )
            }

            // 6. 특수한 URI 패턴 검증 (IPv6 주소 처리 개선)
            validateSpecialUriPatterns(uriString, routeId, uri)

        } catch (e: java.net.URISyntaxException) {
            throw RouteConfigurationException(
                message = "Invalid URI format: '$uriString'. ${e.reason}",
                routeId = routeId,
                cause = e
            )
        } catch (e: RouteConfigurationException) {
            // RouteConfigurationException은 그대로 전파
            throw e
        } catch (e: Exception) {
            throw RouteConfigurationException(
                message = "Invalid URI format: '$uriString'. Unexpected error during URI validation: ${e.message}",
                routeId = routeId,
                cause = e
            )
        }
    }

    /**
     * 네트워크 기반 스키마인지 확인합니다.
     */
    private fun isNetworkScheme(scheme: String): Boolean {
        return scheme.lowercase() in setOf("http", "https", "ftp", "ftps", "ws", "wss")
    }

    /**
     * 문자열에 ASCII가 아닌 문자가 포함되어 있는지 확인합니다.
     */
    private fun containsNonAsciiCharacters(text: String): Boolean {
        return text.any { it.code > 127 }
    }

    /**
     * 네트워크 기반 URI의 호스트를 검증합니다.
     */
    private fun validateNetworkHost(uri: java.net.URI, uriString: String, routeId: String) {
        // 호스트가 null이거나 빈 문자열인 경우 (IDN 처리 전에 확인)
        val originalHost = extractOriginalHost(uriString)
        if (originalHost.isNullOrBlank()) {
            throw RouteConfigurationException(
                message = "Invalid URI format: '$uriString'. Network-based URI must have a valid host",
                routeId = routeId
            )
        }

        // 호스트에 공백 문자 검증 (IPv6 주소가 아닌 경우)
        if (!isIPv6Address(originalHost) && originalHost.contains(" ")) {
            throw RouteConfigurationException(
                message = "Invalid URI format: '$uriString'. Host cannot contain spaces",
                routeId = routeId
            )
        }

        // 국제화 도메인 이름(IDN) 처리
        if (containsNonAsciiCharacters(originalHost)) {
            try {
                // IDN을 ASCII로 변환하여 검증
                val asciiHost = java.net.IDN.toASCII(originalHost)
                log.debug { "Converted IDN host '$originalHost' to ASCII: '$asciiHost'" }
            } catch (e: Exception) {
                throw RouteConfigurationException(
                    message = "Invalid URI format: '$uriString'. Invalid internationalized domain name: ${e.message}",
                    routeId = routeId,
                    cause = e
                )
            }
        }
    }

    /**
     * URI 문자열에서 원본 호스트를 추출합니다 (IDN 변환 전).
     */
    private fun extractOriginalHost(uriString: String): String? {
        return try {
            val schemeEndIndex = uriString.indexOf("://")
            if (schemeEndIndex == -1) return null
            
            val authorityStart = schemeEndIndex + 3
            val pathStart = uriString.indexOf("/", authorityStart).takeIf { it != -1 } ?: uriString.length
            val queryStart = uriString.indexOf("?", authorityStart).takeIf { it != -1 } ?: uriString.length
            val fragmentStart = uriString.indexOf("#", authorityStart).takeIf { it != -1 } ?: uriString.length
            
            val authorityEnd = minOf(pathStart, queryStart, fragmentStart)
            val authority = uriString.substring(authorityStart, authorityEnd)
            
            // 사용자 정보 제거 (user:pass@host 형태)
            val hostPart = if (authority.contains("@")) {
                authority.substringAfter("@")
            } else {
                authority
            }
            
            // 포트 제거 (IPv6 주소 고려)
            if (hostPart.startsWith("[")) {
                // IPv6 주소
                val closeBracket = hostPart.indexOf("]")
                if (closeBracket != -1) {
                    hostPart.substring(0, closeBracket + 1)
                } else {
                    hostPart
                }
            } else {
                // IPv4 주소 또는 도메인 이름
                val colonIndex = hostPart.lastIndexOf(":")
                if (colonIndex != -1 && hostPart.substring(colonIndex + 1).toIntOrNull() != null) {
                    hostPart.substring(0, colonIndex)
                } else {
                    hostPart
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * IPv6 주소인지 확인합니다.
     */
    private fun isIPv6Address(host: String): Boolean {
        return host.startsWith("[") && host.endsWith("]")
    }

    /**
     * 특수한 URI 패턴들을 검증합니다 (IPv6 주소 처리 개선).
     */
    private fun validateSpecialUriPatterns(uriString: String, routeId: String, uri: java.net.URI) {
        // 1. 빈 호스트 패턴 검증 (예: http://, https://)
        if (uriString.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://$"))) {
            throw RouteConfigurationException(
                message = "Invalid URI format: '$uriString'. URI cannot end with '://' without host",
                routeId = routeId
            )
        }

        // 2. 스키마만 있는 패턴 검증 (예: http:, https:)
        if (uriString.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:$"))) {
            throw RouteConfigurationException(
                message = "Invalid URI format: '$uriString'. URI cannot be just a scheme",
                routeId = routeId
            )
        }

        // 3. IPv6 주소가 아닌 경우에만 포트 형식 검증
        if (!isIPv6Address(uri.host ?: "")) {
            // 잘못된 포트 형식 검증 (예: http://host:abc)
            val portPattern = Regex("://([^\\[\\]/]+):([^/]*)")
            val portMatch = portPattern.find(uriString)
            if (portMatch != null) {
                val portString = portMatch.groupValues[2]
                if (portString.isNotEmpty() && portString.toIntOrNull() == null) {
                    throw RouteConfigurationException(
                        message = "Invalid URI format: '$uriString'. Invalid port format: '$portString'",
                        routeId = routeId
                    )
                }
            }

            // 음수 포트 검증 (예: http://host:-1)
            if (uriString.contains(":-")) {
                throw RouteConfigurationException(
                    message = "Invalid URI format: '$uriString'. Port cannot be negative",
                    routeId = routeId
                )
            }
        }

        // 4. 인코딩되지 않은 공백 검증 (원본 URI 문자열에서 확인)
        validateUnEncodedSpaces(uriString, routeId)
    }

    /**
     * 인코딩되지 않은 공백을 검증합니다.
     */
    private fun validateUnEncodedSpaces(uriString: String, routeId: String) {
        // 경로 부분에서 인코딩되지 않은 공백 검증
        val pathStartIndex = uriString.indexOf("/", uriString.indexOf("://") + 3)
        if (pathStartIndex != -1) {
            val pathPart = uriString.substring(pathStartIndex)
            
            // 쿼리 파라미터와 프래그먼트 분리
            val queryIndex = pathPart.indexOf("?")
            val fragmentIndex = pathPart.indexOf("#")
            
            val pathOnly = when {
                queryIndex != -1 && fragmentIndex != -1 -> pathPart.substring(0, minOf(queryIndex, fragmentIndex))
                queryIndex != -1 -> pathPart.substring(0, queryIndex)
                fragmentIndex != -1 -> pathPart.substring(0, fragmentIndex)
                else -> pathPart
            }
            
            // 경로에 인코딩되지 않은 공백이 있는지 확인
            if (pathOnly.contains(" ") && !pathOnly.contains("%20")) {
                throw RouteConfigurationException(
                    message = "Invalid URI format: '$uriString'. URI path contains unencoded spaces. Use %20 for spaces.",
                    routeId = routeId
                )
            }
        }
    }
}