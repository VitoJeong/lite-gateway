package dev.jazzybyte.lite.gateway.exception

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomExceptionsTest {

    @Test
    fun `RouteConfigurationException should include route ID in message when provided`() {
        val routeId = "test-route"
        val originalMessage = "Invalid configuration"
        
        val exception = RouteConfigurationException(originalMessage, routeId)
        
        assertTrue(exception.message!!.contains(routeId))
        assertTrue(exception.message!!.contains(originalMessage))
        assertEquals(routeId, exception.routeId)
    }

    @Test
    fun `RouteConfigurationException should work without route ID`() {
        val originalMessage = "Invalid configuration"
        
        val exception = RouteConfigurationException(originalMessage)
        
        assertTrue(exception.message!!.contains(originalMessage))
        assertEquals(null, exception.routeId)
    }

    @Test
    fun `PredicateInstantiationException should include all context information`() {
        val routeId = "test-route"
        val predicateName = "Path"
        val args = arrayOf("/api/**", "GET")
        val originalMessage = "Constructor not found"
        
        val exception = PredicateInstantiationException(originalMessage, routeId, predicateName, args)
        
        assertTrue(exception.message!!.contains(routeId))
        assertTrue(exception.message!!.contains(predicateName))
        assertTrue(exception.message!!.contains("/api/**"))
        assertTrue(exception.message!!.contains("GET"))
        assertTrue(exception.message!!.contains(originalMessage))
        assertEquals(routeId, exception.routeId)
        assertEquals(predicateName, exception.predicateName)
        assertEquals(args, exception.predicateArgs)
    }

    @Test
    fun `PredicateInstantiationException should work without arguments`() {
        val routeId = "test-route"
        val predicateName = "Path"
        val originalMessage = "Constructor not found"
        
        val exception = PredicateInstantiationException(originalMessage, routeId, predicateName)
        
        assertTrue(exception.message!!.contains(routeId))
        assertTrue(exception.message!!.contains(predicateName))
        assertTrue(exception.message!!.contains(originalMessage))
        assertEquals(null, exception.predicateArgs)
    }

    @Test
    fun `PredicateDiscoveryException should include predicate name and package when provided`() {
        val predicateName = "UnknownPredicate"
        val packageName = "dev.jazzybyte.lite.gateway.predicate"
        val originalMessage = "Class not found"
        
        val exception = PredicateDiscoveryException(originalMessage, predicateName, packageName)
        
        assertTrue(exception.message!!.contains(predicateName))
        assertTrue(exception.message!!.contains(packageName))
        assertTrue(exception.message!!.contains(originalMessage))
        assertEquals(predicateName, exception.predicateName)
        assertEquals(packageName, exception.packageName)
    }

    @Test
    fun `PredicateDiscoveryException should work with minimal information`() {
        val originalMessage = "Discovery failed"
        
        val exception = PredicateDiscoveryException(originalMessage)
        
        assertTrue(exception.message!!.contains(originalMessage))
        assertEquals(null, exception.predicateName)
        assertEquals(null, exception.packageName)
    }
}