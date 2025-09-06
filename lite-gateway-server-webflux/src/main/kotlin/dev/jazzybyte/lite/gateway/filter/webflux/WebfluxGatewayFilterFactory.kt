package dev.jazzybyte.lite.gateway.filter.webflux

import dev.jazzybyte.lite.gateway.config.FilterRegistry
import dev.jazzybyte.lite.gateway.exception.FilterInstantiationException
import dev.jazzybyte.lite.gateway.filter.AddRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.AddResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.FilterDefinition
import dev.jazzybyte.lite.gateway.filter.GatewayFilterFactory
import dev.jazzybyte.lite.gateway.filter.RemoveRequestHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.RemoveResponseHeaderGatewayFilter
import dev.jazzybyte.lite.gateway.filter.core.GatewayFilter
import io.github.oshai.kotlinlogging.KotlinLogging

class WebfluxGatewayFilterFactory : GatewayFilterFactory {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun create(definition: FilterDefinition): GatewayFilter {
        return try {
            validateFilterDefinition(definition)
            createFilterInstance(definition)
        } catch (e: FilterInstantiationException) {
            // Re-throw FilterInstantiationException as-is
            throw e
        } catch (e: Exception) {
            // Wrap other exceptions in FilterInstantiationException
            log.error(e) { "Unexpected error while creating filter '${definition.type}'" }
            throw FilterInstantiationException(
                message = "Unexpected error during filter creation: ${e.message}",
                filterType = definition.type,
                args = definition.args,
                cause = e
            )
        }
    }

    private fun validateFilterDefinition(definition: FilterDefinition) {
        // Validate filter type is not blank
        if (definition.type.isBlank()) {
            throw FilterInstantiationException(
                message = "Filter type cannot be blank",
                filterType = definition.type,
                args = definition.args
            )
        }

        // Check if filter is registered
        if (!FilterRegistry.isFilterRegistered(definition.type)) {
            val availableFilters = FilterRegistry.getAvailableFilterNames()
            throw FilterInstantiationException(
                message = "Filter type '${definition.type}' is not registered. Available filters: $availableFilters",
                filterType = definition.type,
                args = definition.args
            )
        }
    }

    private fun createFilterInstance(definition: FilterDefinition): GatewayFilter {
        val filterClass = FilterRegistry.getFilterClass(definition.type)
        val args = definition.args

        return try {
            when (filterClass) {
                AddRequestHeaderGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name", "value"))
                    AddRequestHeaderGatewayFilter(
                        name = args["name"]!!,
                        value = args["value"]!!
                    )
                }

                RemoveRequestHeaderGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name"))
                    RemoveRequestHeaderGatewayFilter(
                        name = args["name"]!!
                    )
                }

                AddResponseHeaderGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name", "value"))
                    AddResponseHeaderGatewayFilter(
                        name = args["name"]!!,
                        value = args["value"]!!
                    )
                }

                RemoveResponseHeaderGatewayFilter::class.java -> {
                    validateRequiredArgs(definition, listOf("name"))
                    RemoveResponseHeaderGatewayFilter(
                        name = args["name"]!!
                    )
                }

                else -> throw FilterInstantiationException(
                    message = "No factory method available for filter type '${definition.type}'",
                    filterType = definition.type,
                    args = definition.args
                )
            }
        } catch (e: FilterInstantiationException) {
            throw e
        } catch (e: Exception) {
            throw FilterInstantiationException(
                message = "Failed to instantiate filter: ${e.message}",
                filterType = definition.type,
                args = definition.args,
                cause = e
            )
        }
    }

    private fun validateRequiredArgs(definition: FilterDefinition, requiredArgs: List<String>) {
        // First check for completely missing arguments
        val missingArgs = requiredArgs.filter { arg ->
            !definition.args.containsKey(arg) || definition.args[arg] == null
        }

        if (missingArgs.isNotEmpty()) {
            throw FilterInstantiationException(
                message = "Missing required arguments: $missingArgs. Required arguments for '${definition.type}': $requiredArgs",
                filterType = definition.type,
                args = definition.args
            )
        }

        // Then validate argument values for blank strings
        requiredArgs.forEach { arg ->
            val value = definition.args[arg]
            if (value != null && value.isBlank()) {
                throw FilterInstantiationException(
                    message = "Argument '$arg' cannot be blank",
                    filterType = definition.type,
                    args = definition.args
                )
            }
        }
    }
}