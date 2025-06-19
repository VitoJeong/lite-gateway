package dev.jazzybyte.lite.gateway.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebHandler
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

class FilterHandler(
    // 필터
) : WebHandler {
    override fun handle(exchange: ServerWebExchange): Mono<Void> {
        // 필터를 조립하여 진행시킨다.
        // 임시로 '첫 번째 핕터를 진행한다.', '두 번째 핕터를 진행한다.'를 각각 로그로 출력한다.
       return Mono.just(exchange)
                .doOnNext { log.info { "첫 번째 핕터를 진행한다." } }
                .doOnNext { log.info { "두 번째 핕터를 진행한다." } }
                .then()
    }
}