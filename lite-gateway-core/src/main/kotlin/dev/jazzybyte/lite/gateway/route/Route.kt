package dev.jazzybyte.lite.gateway.route

import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import dev.jazzybyte.lite.gateway.predicate.RoutePredicate
import java.net.URI

/**
 * 정의된 라우팅 정보
 */
class Route(
    private val _id: String,
    private val _predicates: List<RoutePredicate>,
    private var _uri: URI,
    // 라우트의 우선순위, 정수 값이 낮을수록 먼저 처리됨
    private val _order: Int,
    private val _filters: List<GatewayFilter>,
) {

    // 라우트의 고유 식별자
    val id: String
        get() = _id

    // 라우트에 정의된 조건 목록
    val predicates: List<RoutePredicate>
        get() = _predicates

    // 라우트가 가리키는 URI
    var uri: URI
        get() = _uri
        private set(value) {
            _uri = value
        }

    // 라우트의 우선순위
    val order: Int
        get() = _order

    val filters: List<GatewayFilter>
        get() = _filters

    init {
        // URI가 null이거나 비어있는 경우 예외 발생
        require(!uri.scheme.isNullOrBlank()) { "The parameter [$uri] format is incorrect, scheme can not be empty" }
        // URI의 scheme이 localhost인 경우 예외 발생
        if (uri.scheme.equals("localhost", ignoreCase = true)) {
            throw IllegalArgumentException("The parameter [$uri] format is incorrect, scheme can not be localhost")
        }
        // 포트 설정이 없는 경우 기본 포트를 설정
        if (uri.port < 0 && uri.scheme.startsWith("http", ignoreCase = true)) {
            val port = if (uri.scheme.equals("https", ignoreCase = true)) 443 else 80

            try {
                this.uri = URI(
                    uri.scheme,
                    uri.userInfo,
                    uri.host,
                    port,
                    uri.path,
                    uri.query,
                    uri.fragment
                )
            } catch (e: Exception) {
                // URI 생성 실패 시 원본 URI 사용 (포트만 설정하지 않음)
                this.uri = uri
            }
        }
    }

    constructor(
        id: String,
        uri: String,
        predicate: RoutePredicate,
        order: Int = Int.MAX_VALUE,
    ) : this(id, listOf(predicate), createUriWithIdnSupport(uri), order, emptyList())

    constructor(
        id: String,
        uri: String,
        predicates: List<RoutePredicate>,
        order: Int = Int.MAX_VALUE,
    ) : this(id, predicates, createUriWithIdnSupport(uri), order, emptyList())

    constructor(
        id: String,
        uri: String,
        predicates: List<RoutePredicate>,
        order: Int = Int.MAX_VALUE,
        filters: List<GatewayFilter>,
    ) : this(id, predicates, createUriWithIdnSupport(uri), order, filters)

    companion object {
        /**
         * IDN 지원과 함께 URI를 생성한다.
         * 매우 긴 경로나 잘못된 IDN의 경우 원본 URI를 사용한다.
         */
        private fun createUriWithIdnSupport(uriString: String): URI {
            return try {
                // 먼저 원본 URI로 파싱 시도
                val originalUri = URI.create(uriString)
                
                // 호스트가 있고 ASCII가 아닌 문자가 포함된 경우에만 IDN 변환 시도
                if (originalUri.host != null && containsNonAsciiCharacters(originalUri.host)) {
                    try {
                        val asciiHost = java.net.IDN.toASCII(originalUri.host)
                        // UriComponentsBuilder를 사용하여 안전하게 URI 생성
                        val normalizedUri = URI(
                            originalUri.scheme,
                            originalUri.userInfo,
                            asciiHost,
                            originalUri.port,
                            originalUri.path,
                            originalUri.query,
                            originalUri.fragment
                        )
                        normalizedUri
                    } catch (e: Exception) {
                        // IDN 변환 실패 시 원본 URI 사용
                        originalUri
                    }
                } else {
                    originalUri
                }
            } catch (e: Exception) {
                // URI 생성 실패 시 예외 전파
                throw e
            }
        }

        /**
         * 문자열에 ASCII가 아닌 문자가 포함되어 있는지 확인한다.
         */
        private fun containsNonAsciiCharacters(text: String): Boolean {
            return text.any { it.code > 127 }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Route

        if (_order != other._order) return false
        if (_id != other._id) return false
        if (_predicates != other._predicates) return false
        if (_uri != other._uri) return false
        if (_filters != other._filters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _order
        result = 31 * result + _id.hashCode()
        result = 31 * result + _predicates.hashCode()
        result = 31 * result + _uri.hashCode()
        result = 31 * result + _filters.hashCode()
        return result
    }

}