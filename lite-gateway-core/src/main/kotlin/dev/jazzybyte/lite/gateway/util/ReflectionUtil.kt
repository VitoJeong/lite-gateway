package dev.jazzybyte.lite.gateway.util

import io.github.classgraph.ClassGraph
import java.lang.reflect.Modifier


class ReflectionUtil {
    companion object {

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