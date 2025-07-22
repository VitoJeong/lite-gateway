package dev.jazzybyte.lite.gateway.util

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter


class ReflectionUtil {
    companion object {
        fun getClassesFromPackage(packageName: String): Set<Class<*>> {
            val scanner = ClassPathScanningCandidateComponentProvider(false)
            scanner.addIncludeFilter(AssignableTypeFilter(Any::class.java))

            val candidates = scanner.findCandidateComponents(packageName)
            return candidates
                .mapNotNull { it.beanClassName }
                .filterNot { className ->
                    className.contains("test", ignoreCase = true) || className.contains("Test")
                }
                .map { Class.forName(it) }
                .toSet()
        }

        fun <T> createInstanceOfType(type: Class<T>): T {
            return type.getDeclaredConstructor().newInstance() // 기본 생성자를 사용하여 인스턴스 생성
        }

        @Suppress("UNCHECKED_CAST")
        fun <T, U> createInstanceOfType(type: Class<T>, vararg args: U): T {
            val constructor = type.constructors.find { it.parameterCount == args.size }
                ?: throw IllegalArgumentException("No suitable constructor found for ${type.name} with ${args.size} parameters.")

            return constructor.newInstance(*args) as T
        }

        @Suppress("UNCHECKED_CAST")
        fun <T, U> createInstanceOfType(type: Class<T>, arg: U): T {
            val constructor = type.constructors.find { it.parameterCount == 1 }
                ?: throw IllegalArgumentException("No suitable constructor found for ${type.name} with one parameter.")

            return constructor.newInstance(arg) as T
        }

    }
}