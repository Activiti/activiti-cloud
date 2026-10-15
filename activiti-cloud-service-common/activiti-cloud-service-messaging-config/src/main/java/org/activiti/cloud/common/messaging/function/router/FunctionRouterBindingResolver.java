/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.common.messaging.function.router;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamFunctionProperties;

public class FunctionRouterBindingResolver {

    private final StreamFunctionProperties streamFunctionProperties;

    private final BindingServiceProperties bindingServiceProperties;

    public FunctionRouterBindingResolver(
        StreamFunctionProperties streamFunctionProperties,
        BindingServiceProperties bindingServiceProperties
    ) {
        this.streamFunctionProperties = streamFunctionProperties;
        this.bindingServiceProperties = bindingServiceProperties;
    }

    public List<String> getFunctionNamesForDestination(String destination) {
        return getConsumerBindingsForDestination(destination).stream().map(this::getFunctionNameFromBinding).toList();
    }

    /**
     * Finds all active consumer binding names pointing to a specific broker destination.
     *
     * @param targetDestination The broker topic or exchange name (e.g., "orders-topic")
     * @return A list of matching consumer binding names (e.g., ["processOrders-in-0"])
     */
    public List<String> getConsumerBindingsForDestination(String targetDestination) {
        if (targetDestination == null || targetDestination.isBlank()) {
            return List.of();
        }

        // Get all configured bindings across the entire application context
        Map<String, BindingProperties> allBindings = bindingServiceProperties.getBindings();

        return allBindings
            .entrySet()
            .stream()
            // 1. Isolate consumer bindings (rejecting producer-only bindings)
            .filter(entry -> isConsumerBinding(entry.getKey(), entry.getValue()))
            // 2. Exact or multi-destination match on target broker name
            .filter(entry -> matchesDestination(entry.getValue().getDestination(), targetDestination))
            // 3. Collect the logical binding names
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Determines if a binding acts as an input channel/consumer.
     */
    private boolean isConsumerBinding(String bindingName, BindingProperties properties) {
        // Fallback check on standard function-naming suffix pattern if property is null
        if (properties.getConsumer() == null) {
            return (
                bindingName.contains("-in-") ||
                streamFunctionProperties
                    .getBindings()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().contains("-in-"))
                    .filter(entry -> bindingName.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .isPresent()
            );
        }
        return true;
    }

    /**
     * Validates if the binding property matches our search criteria.
     * Handles single strings as well as comma-separated multi-destination consumers.
     */
    private boolean matchesDestination(String bindingDestination, String targetDestination) {
        if (bindingDestination == null) {
            return false;
        }

        // Consumer bindings can technically accept comma-separated destinations
        if (bindingDestination.contains(",")) {
            String[] destinations = bindingDestination.split(",");
            for (String dest : destinations) {
                if (dest.trim().equals(targetDestination)) {
                    return true;
                }
            }
            return false;
        }

        return bindingDestination.trim().equals(targetDestination);
    }

    /**
     * Determines the target Spring Cloud Function name from a provided binding name.
     * Handles both custom (aliased) bindings and default functional naming conventions.
     *
     * @param runtimeBindingName The binding name to evaluate (e.g., "ordersInputChannel" or "uppercase-in-0")
     * @return The underlying function bean name
     */
    public String getFunctionNameFromBinding(String runtimeBindingName) {
        if (runtimeBindingName == null || runtimeBindingName.isBlank()) {
            return "Unknown Function";
        }

        // 1. Fetch explicit 'spring.cloud.stream.function.bindings' maps from the environment
        Map<String, String> functionBindings = streamFunctionProperties.getBindings();

        // 2. Loop through the map to check if the custom name matches the value
        // Key = default convention name (e.g., "myFunc-in-0"), Value = custom alias (e.g., "ordersInputChannel")
        String matchedDefaultBinding = functionBindings
            .entrySet()
            .stream()
            .filter(entry -> runtimeBindingName.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(runtimeBindingName); // Fallback to itself if no alias was matched

        // 3. Fallback to convention parsing on the resulting default string
        return parseConventionName(matchedDefaultBinding);
    }

    private String parseConventionName(String bindingName) {
        if (bindingName.contains("-in-")) {
            return bindingName.split("-in-")[0];
        } else if (bindingName.contains("-out-")) {
            return bindingName.split("-out-")[0];
        }
        return bindingName; // Returns original string if it doesn't match convention format
    }
}
