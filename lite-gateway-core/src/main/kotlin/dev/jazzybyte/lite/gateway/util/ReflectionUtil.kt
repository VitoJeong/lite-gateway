package dev.jazzybyte.lite.gateway.util

import io.github.classgraph.ClassGraph
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap


class ReflectionUtil {
    companion object {
        
        // Constructor caching for performance optimization
        private val constructorCache = ConcurrentHashMap<String, Constructor<*>>()
        private val noArgsConstructorCache = ConcurrentHashMap<Class<*>, Constructor<*>>()

        @Suppress("UNCHECKED_CAST")
        fun <T> findClassesOfType(packageName: String, type: Class<T>): List<Class<out T>> {
            val scanResult = ClassGraph()
                .enableClassInfo()
                .acceptPackages(packageName)
                .scan()

            return scanResult.getClassesImplementing(type.name) // 또는 getSubclasses(type.name)
                .loadClasses()
                .filter { !it.isInterface && !Modifier.isAbstract(it.modifiers) }
                .map { it as Class<out T> }
        }

        fun <T> createInstanceOfType(type: Class<T>): T {
            val constructor = noArgsConstructorCache.computeIfAbsent(type) { clazz ->
                clazz.getDeclaredConstructor()
            }
            @Suppress("UNCHECKED_CAST")
            return constructor.newInstance() as T
        }

        @Suppress("UNCHECKED_CAST")
        fun <T, U> createInstanceOfType(type: Class<T>, vararg args: U): T {
            val cacheKey = "${type.name}:${args.size}"
            val constructor = constructorCache.computeIfAbsent(cacheKey) { _ ->
                type.constructors.find { it.parameterCount == args.size }
                    ?: throw IllegalArgumentException("No suitable constructor found for ${type.name} with ${args.size} parameters.")
            }

            return constructor.newInstance(*args) as T
        }

        @Suppress("UNCHECKED_CAST")
        fun <T, U> createInstanceOfType(type: Class<T>, arg: U): T {
            val cacheKey = "${type.name}:1"
            val constructor = constructorCache.computeIfAbsent(cacheKey) { _ ->
                type.constructors.find { it.parameterCount == 1 }
                    ?: throw IllegalArgumentException("No suitable constructor found for ${type.name} with one parameter.")
            }

            return constructor.newInstance(arg) as T
        }
        
        /**
         * Clear constructor caches - useful for testing or memory management
         */
        fun clearConstructorCaches() {
            constructorCache.clear()
            noArgsConstructorCache.clear()
        }
        
        /**
         * Get cache statistics for monitoring
         */
        fun getCacheStatistics(): Map<String, Int> {
            return mapOf(
                "constructorCacheSize" to constructorCache.size,
                "noArgsConstructorCacheSize" to noArgsConstructorCache.size
            )
        }
    }
}