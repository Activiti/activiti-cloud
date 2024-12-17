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
    }
}
