package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Resilience4j를 사용한 회로차단 패턴 필터
 * 
 * @param name 회로차단기 이름
 * @param failureRateThreshold 실패율 임계값 (퍼센트, 기본값: 50%)
 * @param waitDurationInOpenState 열린 상태에서 대기 시간 (기본값: 60초)
 * @param slidingWindowSize 슬라이딩 윈도우 크기 (기본값: 100)
 * @param fallbackResponse 폴백 응답 메시지 (기본값: "Service Unavailable")
 */
class CircuitBreakerGatewayFilter(
    private val name: String,
    private val failureRateThreshold: Float = 50.0f,
    private val waitDurationInOpenState: Duration = Duration.ofSeconds(60),
    private val slidingWindowSize: Int = 100,
    private val fallbackResponse: String = "Service Unavailable"
) : GatewayFilter {

    companion object {
        private val log = KotlinLogging.logger {}
        private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()
    }

    private val circuitBreaker: CircuitBreaker

    init {
        require(name.isNotBlank()) { "Circuit breaker name cannot be blank" }
        require(failureRateThreshold in 0.0f..100.0f) { 
            "Failure rate threshold must be between 0 and 100, but was: $failureRateThreshold" 
        }
        require(slidingWindowSize > 0) { 
            "Sliding window size must be greater than 0, but was: $slidingWindowSize" 
        }
        require(!waitDurationInOpenState.isNegative) { 
            "Wait duration cannot be negative, but was: $waitDurationInOpenState" 
        }

        circuitBreaker = circuitBreakers.computeIfAbsent(name) {
            createCircuitBreaker(name)
        }
    }

    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        val webfluxContext = context as WebFluxGatewayContext
        
        log.debug { 
            "Circuit breaker '$name' state: ${circuitBreaker.state}, " +
            "failure rate: ${circuitBreaker.metrics.failureRate}%" 
        }

        return chain.filter(context)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume { throwable ->
                log.warn(throwable) { "Circuit breaker '$name' triggered fallback due to: ${throwable.message}" }
                handleFallback(webfluxContext, throwable)
            }
    }

    /**
     * 회로차단기를 생성한다
     */
    private fun createCircuitBreaker(name: String): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .waitDurationInOpenState(waitDurationInOpenState)
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(10) // 최소 호출 수
            .permittedNumberOfCallsInHalfOpenState(3) // 반열림 상태에서 허용되는 호출 수
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build()

        val circuitBreaker = CircuitBreaker.of(name, config)
        
        // 이벤트 리스너 등록
        circuitBreaker.eventPublisher
            .onStateTransition { event ->
                log.info { 
                    "Circuit breaker '$name' state transition: ${event.stateTransition.fromState} -> ${event.stateTransition.toState}" 
                }
            }
            .onCallNotPermitted { event ->
                log.warn { "Circuit breaker '$name' call not permitted" }
            }
            .onFailureRateExceeded { event ->
                log.warn { 
                    "Circuit breaker '$name' failure rate exceeded: ${event.failureRate}% (threshold: $failureRateThreshold%)" 
                }
            }

        return circuitBreaker
    }

    /**
     * 폴백 응답을 처리한다
     */
    private fun handleFallback(context: WebFluxGatewayContext, throwable: Throwable): Mono<Void> {
        val response = context.exchange.response
        
        // 회로차단기 상태에 따른 HTTP 상태 코드 설정
        response.statusCode = when (circuitBreaker.state) {
            CircuitBreaker.State.OPEN -> HttpStatus.SERVICE_UNAVAILABLE
            CircuitBreaker.State.HALF_OPEN -> HttpStatus.SERVICE_UNAVAILABLE
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        // 응답 헤더 설정
        response.headers.add("Content-Type", MediaType.TEXT_PLAIN_VALUE)
        response.headers.add("X-Circuit-Breaker-Name", name)
        response.headers.add("X-Circuit-Breaker-State", circuitBreaker.state.toString())
        response.headers.add("X-Circuit-Breaker-Failure-Rate", circuitBreaker.metrics.failureRate.toString())

        // 폴백 응답 본문 작성
        val buffer = response.bufferFactory().wrap(fallbackResponse.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }
}