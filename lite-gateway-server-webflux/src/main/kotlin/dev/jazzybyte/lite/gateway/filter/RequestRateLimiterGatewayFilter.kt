package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 토큰 버킷 알고리즘을 사용한 요청 속도 제한 필터
 * 
 * @param replenishRate 초당 토큰 보충 속도
 * @param burstCapacity 버킷의 최대 용량 (버스트 허용량)
 * @param requestedTokens 요청당 소모할 토큰 수 (기본값: 1)
 */
class RequestRateLimiterGatewayFilter(
    private val replenishRate: Double,
    private val burstCapacity: Long,
    private val requestedTokens: Long = 1L
) : GatewayFilter {

    companion object {
        private val log = KotlinLogging.logger {}
        private val buckets = ConcurrentHashMap<String, TokenBucket>()
    }

    init {
        require(replenishRate > 0) { "Replenish rate must be greater than 0, but was: $replenishRate" }
        require(burstCapacity > 0) { "Burst capacity must be greater than 0, but was: $burstCapacity" }
        require(requestedTokens > 0) { "Requested tokens must be greater than 0, but was: $requestedTokens" }
        require(burstCapacity >= requestedTokens) { 
            "Burst capacity ($burstCapacity) must be greater than or equal to requested tokens ($requestedTokens)" 
        }
    }

    override fun filter(context: GatewayContext, chain: GatewayFilterChain): Mono<Void> {
        val webfluxContext = context as WebFluxGatewayContext
        val request = webfluxContext.exchange.request
        
        // 클라이언트 식별자 생성 (IP 주소 기반)
        val clientId = getClientId(webfluxContext)
        
        log.debug { "Rate limiting check for client: $clientId, requested tokens: $requestedTokens" }

        val bucket = buckets.computeIfAbsent(clientId) { 
            TokenBucket(replenishRate, burstCapacity)
        }

        return if (bucket.tryConsume(requestedTokens)) {
            log.debug { "Rate limit passed for client: $clientId" }
            chain.filter(context)
        } else {
            log.warn { "Rate limit exceeded for client: $clientId" }
            handleRateLimitExceeded(webfluxContext)
        }
    }

    /**
     * 클라이언트 식별자를 생성한다
     */
    private fun getClientId(context: WebFluxGatewayContext): String {
        val request = context.exchange.request
        
        // X-Forwarded-For 헤더에서 실제 클라이언트 IP 추출
        val forwardedFor = request.headers.getFirst("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.split(",")[0].trim()
        }
        
        // X-Real-IP 헤더 확인
        val realIp = request.headers.getFirst("X-Real-IP")
        if (!realIp.isNullOrBlank()) {
            return realIp
        }
        
        // 직접 연결된 클라이언트 IP
        return request.remoteAddress?.address?.hostAddress ?: "unknown"
    }

    /**
     * 속도 제한 초과 시 처리
     */
    private fun handleRateLimitExceeded(context: WebFluxGatewayContext): Mono<Void> {
        val response = context.exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        
        // Rate limit 관련 헤더 추가
        response.headers.add("X-RateLimit-Replenish-Rate", replenishRate.toString())
        response.headers.add("X-RateLimit-Burst-Capacity", burstCapacity.toString())
        response.headers.add("X-RateLimit-Requested-Tokens", requestedTokens.toString())
        
        return response.setComplete()
    }

    /**
     * 토큰 버킷 구현
     */
    private class TokenBucket(
        private val replenishRate: Double,
        private val capacity: Long
    ) {
        private val tokens = AtomicLong(capacity)
        private var lastRefillTime = Instant.now()

        /**
         * 지정된 수의 토큰을 소모하려고 시도한다
         */
        @Synchronized
        fun tryConsume(tokensRequested: Long): Boolean {
            refill()
            
            val currentTokens = tokens.get()
            return if (currentTokens >= tokensRequested) {
                tokens.addAndGet(-tokensRequested)
                true
            } else {
                false
            }
        }

        /**
         * 토큰을 보충한다
         */
        private fun refill() {
            val now = Instant.now()
            val timePassed = Duration.between(lastRefillTime, now)
            val tokensToAdd = (timePassed.toMillis() / 1000.0 * replenishRate).toLong()
            
            if (tokensToAdd > 0) {
                val currentTokens = tokens.get()
                val newTokens = minOf(capacity, currentTokens + tokensToAdd)
                tokens.set(newTokens)
                lastRefillTime = now
            }
        }
    }
}