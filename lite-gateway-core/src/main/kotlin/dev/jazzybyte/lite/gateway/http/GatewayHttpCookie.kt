package dev.jazzybyte.lite.gateway.http

class GatewayHttpCookie(val name: String, val value: String) {

    override fun toString(): String {
        return "Cookie($name=$value)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GatewayHttpCookie) return false

        if (name != other.name) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}