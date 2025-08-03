package dev.jazzybyte.lite.gateway.exception

/**
 * Exception thrown when a predicate instance cannot be created.
 * This exception provides detailed information about the predicate that failed to instantiate,
 * including the route context and predicate arguments.
 *
 * @param message The error message describing the instantiation issue
 * @param routeId The ID of the route containing the predicate that failed to instantiate
 * @param predicateName The name of the predicate that failed to instantiate
 * @param predicateArgs The arguments that were provided to the predicate constructor (optional)
 * @param cause The underlying cause of the exception (optional)
 */
class PredicateInstantiationException(
    message: String,
    val routeId: String,
    val predicateName: String,
    val predicateArgs: Array<String>? = null,
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, routeId, predicateName, predicateArgs), cause) {

    companion object {
        private fun buildMessage(
            message: String,
            routeId: String,
            predicateName: String,
            predicateArgs: Array<String>?
        ): String {
            val argsInfo = if (predicateArgs != null && predicateArgs.isNotEmpty()) {
                " with arguments [${predicateArgs.joinToString(", ")}]"
            } else {
                ""
            }
            
            return "Failed to instantiate predicate '$predicateName' for route '$routeId'$argsInfo: $message"
        }
    }
}