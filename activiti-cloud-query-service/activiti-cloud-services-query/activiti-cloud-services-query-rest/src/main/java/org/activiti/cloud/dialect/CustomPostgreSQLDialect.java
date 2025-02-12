/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.dialect;

import java.math.BigDecimal;
import java.util.Map;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * Custom PostgreSQL dialect to add custom JSONB functions that abstract the PostgreSQL JSON operators.
 */
public class CustomPostgreSQLDialect extends PostgreSQLDialect {

    /**
     * Extracts the "value" field from a JSONB column and casts it to STRING.
     */
    public static final String EXTRACT_JSON_STRING_VALUE = "jsonb_string_value_extract";

    /**
     * Extracts the "value" field from a JSONB column and casts it to BOOLEAN.
     */
    public static final String EXTRACT_JSON_BOOLEAN_VALUE = "jsonb_boolean_value_extract";
    /**
     * Extracts the "value" field from a JSONB column and casts it to NUMERIC with a precision of 38 and a scale of 16.
     */
    public static final String EXTRACT_JSON_NUMERIC_VALUE = "jsonb_numeric_value_extract";

    private static final Map<Class<?>, String> extractionFunctionsByType = Map.of(
        String.class,
        EXTRACT_JSON_STRING_VALUE,
        Boolean.class,
        EXTRACT_JSON_BOOLEAN_VALUE,
        BigDecimal.class,
        EXTRACT_JSON_NUMERIC_VALUE
    );

    public static String getExtractionFunctionName(Class<?> type) {
        return extractionFunctionsByType.getOrDefault(type, EXTRACT_JSON_STRING_VALUE);
    }

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        functionRegistry
            .patternDescriptorBuilder(EXTRACT_JSON_STRING_VALUE, "?1->>'value'")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.STRING)
            )
            .setExactArgumentCount(1)
            .setArgumentListSignature("JSONB jsonb")
            .register();
        functionRegistry
            .patternDescriptorBuilder(EXTRACT_JSON_BOOLEAN_VALUE, "(?1->>'value')::BOOLEAN")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(1)
            .setArgumentListSignature("JSONB jsonb")
            .register();
        functionRegistry
            .patternDescriptorBuilder(EXTRACT_JSON_NUMERIC_VALUE, "(?1->>'value')::NUMERIC(38,16)")
            .setInvariantType(
                functionContributions
                    .getTypeConfiguration()
                    .getBasicTypeRegistry()
                    .resolve(StandardBasicTypes.BIG_DECIMAL)
            )
            .setExactArgumentCount(1)
            .setArgumentListSignature("JSONB jsonb")
            .register();
    }
}
