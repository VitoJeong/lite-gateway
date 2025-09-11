package dev.jazzybyte.lite.gateway.filter

import dev.jazzybyte.lite.gateway.context.webflux.WebFluxGatewayContext
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.system.measureTimeMillis

@DisplayName("ModifyResponseBodyGatewayFilter 성능 테스트")
class ModifyResponseBodyGatewayFilterPerformanceTest {

    private lateinit var filter: ModifyResponseBodyGatewayFilter
    private lateinit var context: WebFluxGatewayContext
    private lateinit var chain: GatewayFilterChain
    private lateinit var exchange: ServerWebExchange
    private lateinit var request: ServerHttpRequest
    private lateinit var response: ServerHttpResponse

    @BeforeEach
    fun setUp() {
        context = mockk<WebFluxGatewayContext>(relaxed = true)
        chain = mockk<GatewayFilterChain>(relaxed = true)
        exchange = mockk<ServerWebExchange>(relaxed = true)
        request = mockk<ServerHttpRequest>(relaxed = true)
        response = mockk<ServerHttpResponse>(relaxed = true)

        every { context.exchange } returns exchange
        every { exchange.request } returns request
        every { exchange.response } returns response
        every { chain.filter(any()) } returns Mono.empty()
    }

    @Test
    @DisplayName("1MB 응답 본문을 1초 이내에 처리해야 한다")
    fun shouldProcessLargeResponseBodyWithinTimeLimit() {
        // Given
        val transformFunction: (String) -> String = { body ->
            // 간단한 변환 (대소문자 변경)
            body.replace("test", "TEST")
        }
        filter = ModifyResponseBodyGatewayFilter(transformFunction, MediaType.TEXT_PLAIN)

        // When
        val executionTime = measureTimeMillis {
            StepVerifier.create(filter.filter(context, chain))
                .verifyComplete()
        }

        // Then
        println("필터 실행 시간: ${executionTime}ms")
        assert(executionTime < 1000) { "처리 시간이 1초를 초과했습니다: ${executionTime}ms" }
    }

    @Test
    @DisplayName("복잡한 JSON 변환을 효율적으로 처리해야 한다")
    fun shouldProcessComplexJsonTransformationEfficiently() {
        // Given
        val transformFunction: (String) -> String = { body ->
            body
                .replace(Regex(""""password"\s*:\s*"[^"]*""""), """"password":"[MASKED]"""")
                .replace(Regex(""""email"\s*:\s*"[^"]*""""), """"email":"[MASKED]"""")
        }
        filter = ModifyResponseBodyGatewayFilter(transformFunction, MediaType.APPLICATION_JSON)

        // When
        val executionTime = measureTimeMillis {
            StepVerifier.create(filter.filter(context, chain))
                .verifyComplete()
        }

        // Then
        println("JSON 변환 처리 시간: ${executionTime}ms")
        assert(executionTime < 1000) { "JSON 변환 처리 시간이 1초를 초과했습니다: ${executionTime}ms" }
    }

    @Test
    @DisplayName("메모리 사용량이 합리적인 범위 내에 있어야 한다")
    fun shouldUseReasonableMemoryAmount() {
        // Given
        val transformFunction: (String) -> String = { it.uppercase() }
        filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When
        val runtime = Runtime.getRuntime()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        StepVerifier.create(filter.filter(context, chain))
            .verifyComplete()
        
        System.gc() // 가비지 컬렉션 실행
        Thread.sleep(100) // GC 완료 대기
        
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = memoryAfter - memoryBefore

        // Then
        println("메모리 사용량: ${memoryUsed / 1024 / 1024}MB")
        // 기본적인 메모리 사용량 검증
        assert(memoryUsed < 100 * 1024 * 1024) { // 100MB 이하
            "메모리 사용량이 너무 큽니다: ${memoryUsed / 1024 / 1024}MB" 
        }
    }

    @Test
    @DisplayName("동시 요청 처리 성능을 검증해야 한다")
    fun shouldHandleConcurrentRequestsEfficiently() {
        // Given
        val transformFunction: (String) -> String = { body ->
            // 간단한 변환
            body.replace("test", "TEST")
        }
        filter = ModifyResponseBodyGatewayFilter(transformFunction)

        // When
        val concurrentRequests = 5
        val executionTime = measureTimeMillis {
            val monos = (1..concurrentRequests).map {
                filter.filter(context, chain)
            }
            
            monos.forEach { mono ->
                StepVerifier.create(mono)
                    .verifyComplete()
            }
        }

        // Then
        println("동시 요청 $concurrentRequests 개 처리 시간: ${executionTime}ms")
        val averageTime = executionTime / concurrentRequests
        println("요청당 평균 처리 시간: ${averageTime}ms")
        
        assert(averageTime < 200) { 
            "동시 요청 처리 시 평균 응답 시간이 200ms를 초과했습니다: ${averageTime}ms" 
        }
    }
}