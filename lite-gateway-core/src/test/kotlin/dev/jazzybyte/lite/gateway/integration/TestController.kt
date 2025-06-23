package dev.jazzybyte.lite.gateway.integration

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TestController {

    @GetMapping("/users")
    fun exact(): String {
        return "GET users Match"
    }

    @PostMapping("/users")
    fun test(): String {
        return "POST users Match"
    }

    @GetMapping("/param")
    fun testWithQueryParam(@RequestParam("param") param: String): String {
        return "Query param: $param"
    }

}