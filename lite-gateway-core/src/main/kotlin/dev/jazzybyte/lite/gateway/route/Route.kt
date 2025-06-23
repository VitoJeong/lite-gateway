package dev.jazzybyte.lite.gateway.route

import org.springframework.core.Ordered
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * 정의된 라우팅 정보
 *
 * @property id The unique identifier for the route.
 * @property uri The URI to which the route points.
 */
class Route(
    private val _id: String,
    private val _predicates: List<RoutePredicate>,
    private var _uri: URI,
    // 라우트의 우선순위, 정수 값이 낮을수록 먼저 처리됨
    private val _order: Int = Ordered.LOWEST_PRECEDENCE,
) {

    // 라우트의 고유 식별자
    val id: String get() = _id

    // 라우트에 정의된 조건 목록
    val predicates: List<RoutePredicate> get() = _predicates

    // 라우트가 가리키는 URI
    var uri: URI
        get() = _uri
        private set(value) {
            _uri = value
        }

    // 라우트의 우선순위
    val order: Int get() = _order

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
            uri = UriComponentsBuilder.fromUri(uri).port(port).build(false).toUri()
        }
    }

    constructor(
        id: String,
        predicate: RoutePredicate,
        uri: String,
        order: Int = Ordered.LOWEST_PRECEDENCE
    ) : this(id, listOf(predicate), URI.create(uri), order)

    constructor(
        id: String,
        uri: String,
        order: Int = Ordered.LOWEST_PRECEDENCE,
        predicates: List<RoutePredicate>,
    ) : this(id, predicates, URI.create(uri), order)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Route

        if (_order != other._order) return false
        if (_id != other._id) return false
        if (_predicates != other._predicates) return false
        if (_uri != other._uri) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _order
        result = 31 * result + _id.hashCode()
        result = 31 * result + _predicates.hashCode()
        result = 31 * result + _uri.hashCode()
        return result
    }

}