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
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.StringJavaType;

/**
 * Custom PostgreSQL dialect to add custom JSONB functions that abstract the PostgreSQL JSON path operators:
 * All functions accept exactly two arguments:
 * 1. A JSONB column field or a SQL path to a JSONB field.
 * 2. A value to compare with the extracted field "value" from the JSONB field.
 */
public class CustomPostgreSQLDialect extends PostgreSQLDialect {

    /**
     * Extracts the "value" field from a JSONB column and compares it with the given value, returning true if they are equal.
     * It works with strings and booleans.
     */
    public static final String JSON_VALUE_EQUALS = "jsonb_value_eq";

    /**
     * Extracts the "value" field from a JSONB column, casts it to NUMERIC and compares it with the given value cast to NUMERIC,
     * returning true if they are equal.
     */
    public static final String JSON_VALUE_NUMERIC_EQUALS = "jsonb_numeric_eq";

    /**
     * Extracts the "value" field from a JSONB column, casts it to NUMERIC and compares it with the given value cast to NUMERIC,
     * returning true if the extracted value is greater than the given value.
     */
    public static final String JSON_VALUE_NUMERIC_GREATER_THAN = "jsonb_numeric_gt";

    /**
     * Extracts the "value" field from a JSONB column, casts it to NUMERIC and compares it with the given value cast to NUMERIC,
     * returning true if the extracted value is greater than or equal to the given value.
     */
    public static final String JSON_VALUE_NUMERIC_GREATER_THAN_EQUAL = "jsonb_numeric_gte";

    /**
     * Extracts the "value" field from a JSONB column, casts it to NUMERIC and compares it with the given value cast to NUMERIC,
     * returning true if the extracted value is less than the given value.
     */
    public static final String JSON_VALUE_NUMERIC_LESS_THAN = "jsonb_numeric_lt";

    /**
     * Extracts the "value" field from a JSONB column, casts it to NUMERIC and compares it with the given value cast to NUMERIC,
     * returning true if the extracted value is less than or equal to the given value.
     */
    public static final String JSON_VALUE_NUMERIC_LESS_THAN_EQUAL = "jsonb_numeric_lte";

    /**
     * Extracts the "value" field from a JSONB column and compares it with the given value using a case-sensitive regex pattern,
     * returning true if the extracted value matches the pattern. The "value" field must contain a string.
     */
    public static final String JSON_VALUE_LIKE_CASE_SENSITIVE = "jsonb_value_like_cs";

    /**
     * Extracts the "value" field from a JSONB column and compares it with the given value using a case-insensitive regex pattern,
     * returning true if the extracted value matches the pattern. The "value" field must contain a string.
     */
    public static final String JSON_VALUE_LIKE_CASE_INSENSITIVE = "jsonb_value_like_ci";

    /**
     * Extracts the "value" field from a JSONB column, casts it to DATE and compares it with the given value cast to DATE,
     * returning true if they are equal.
     */
    public static final String JSON_VALUE_DATE_EQUALS = "jsonb_date_eq";

    /**
     * Extracts the "value" field from a JSONB column, casts it to DATE and compares it with the given value cast to DATE,
     * returning true if the extracted value is greater than the given value.
     */
    public static final String JSON_VALUE_DATE_GREATER_THAN = "jsonb_date_gt";

    /**
     * Extracts the "value" field from a JSONB column, casts it to DATE and compares it with the given value cast to DATE,
     * returning true if the extracted value is greater than or equal to the given value.
     */
    public static final String JSON_VALUE_DATE_GREATER_THAN_EQUAL = "jsonb_date_gte";

    /**
     * Extracts the "value" field from a JSONB column, casts it to DATE and compares it with the given value cast to DATE,
     * returning true if the extracted value is less than the given value.
     */
    public static final String JSON_VALUE_DATE_LESS_THAN = "jsonb_date_lt";

    /**
     * Extracts the "value" field from a JSONB column, casts it to DATE and compares it with the given value cast to DATE,
     * returning true if the extracted value is less than or equal to the given value.
     */
    public static final String JSON_VALUE_DATE_LESS_THAN_EQUAL = "jsonb_date_lte";

    /**
     * Extracts the "value" field from a JSONB column, casts it to TIMESTAMPTZ and compares it with the given value cast to TIMESTAMPTZ,
     * returning true if they are equal.
     */
    public static final String JSON_VALUE_DATETIME_EQUALS = "jsonb_datetime_eq";

    /**
     * Extracts the "value" field from a JSONB column, casts it to TIMESTAMPTZ and compares it with the given value cast to TIMESTAMPTZ,
     * returning true if the extracted value is greater than the given value.
     */
    public static final String JSON_VALUE_DATETIME_GREATER_THAN = "jsonb_datetime_gt";

    /**
     * Extracts the "value" field from a JSONB column, casts it to TIMESTAMPTZ and compares it with the given value cast to TIMESTAMPTZ,
     * returning true if the extracted value is greater than or equal to the given value.
     */
    public static final String JSON_VALUE_DATETIME_GREATER_THAN_EQUAL = "jsonb_datetime_gte";

    /**
     * Extracts the "value" field from a JSONB column, casts it to TIMESTAMPTZ and compares it with the given value cast to TIMESTAMPTZ,
     * returning true if the extracted value is less than the given value.
     */
    public static final String JSON_VALUE_DATETIME_LESS_THAN = "jsonb_datetime_lt";

    /**
     * Extracts the "value" field from a JSONB column, casts it to TIMESTAMPTZ and compares it with the given value cast to TIMESTAMPTZ,
     * returning true if the extracted value is less than or equal to the given value.
     */
    public static final String JSON_VALUE_DATETIME_LESS_THAN_EQUAL = "jsonb_datetime_lte";

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        registerJsonValueEquals(functionContributions);
        registerJsonValueLikeFunctions(functionContributions);
        registerJsonValueNumericFunctions(functionContributions);
        registerJsonValueDateFunctions(functionContributions);
        registerJsonValueDatetimeFunctions(functionContributions);
    }

    private void registerJsonValueEquals(FunctionContributions functionContributions) {
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_EQUALS, "?1 @@ '$.value == ?2'")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, ANY value")
            .setArgumentTypeResolver((function, argIndex, converter) -> {
                if (argIndex == 1 && function.getArguments().get(1).getNodeJavaType().equals(StringJavaType.INSTANCE)) {
                    return new DoubleQuotedStringType();
                }
                return StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE.resolveFunctionArgumentType(
                    function,
                    argIndex,
                    converter
                );
            })
            .register();
    }

    private void registerJsonValueLikeFunctions(FunctionContributions functionContributions) {
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_LIKE_CASE_SENSITIVE, "?1 @@ '$.value like_regex ?2'")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .setArgumentTypeResolver((function, argIndex, converter) -> {
                if (argIndex == 1 && function.getArguments().get(1).getNodeJavaType().equals(StringJavaType.INSTANCE)) {
                    return new DoubleQuotedStringType(RelationalFormType.LIKE_CASE_SENSITIVE);
                }
                return StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE.resolveFunctionArgumentType(
                    function,
                    argIndex,
                    converter
                );
            })
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_LIKE_CASE_INSENSITIVE, "?1 @@ '$.value like_regex ?2'")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .setArgumentTypeResolver((function, argIndex, converter) -> {
                if (argIndex == 1 && function.getArguments().get(1).getNodeJavaType().equals(StringJavaType.INSTANCE)) {
                    return new DoubleQuotedStringType(RelationalFormType.LIKE_CASE_INSENSITIVE);
                }
                return StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE.resolveFunctionArgumentType(
                    function,
                    argIndex,
                    converter
                );
            })
            .register();
    }

    public void registerJsonValueNumericFunctions(FunctionContributions functionContributions) {
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_NUMERIC_EQUALS, "(?1->>'value')::NUMERIC = ?2")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_NUMERIC_GREATER_THAN, "(?1->>'value')::NUMERIC > ?2")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_NUMERIC_GREATER_THAN_EQUAL, "(?1->>'value')::NUMERIC >= ?2")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_NUMERIC_LESS_THAN, "(?1->>'value')::NUMERIC < ?2")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_NUMERIC_LESS_THAN_EQUAL, "(?1->>'value')::NUMERIC <= ?2")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
    }

    private void registerJsonValueDateFunctions(FunctionContributions functionContributions) {
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_DATE_EQUALS, "(?1->>'value')::DATE = ?2::DATE")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_DATE_GREATER_THAN, "(?1->>'value')::DATE > ?2::DATE")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_DATE_GREATER_THAN_EQUAL, "(?1->>'value')::DATE >= ?2::DATE")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_DATE_LESS_THAN, "(?1->>'value')::DATE < ?2::DATE")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_DATE_LESS_THAN_EQUAL, "(?1->>'value')::DATE <= ?2::DATE")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
    }

    private void registerJsonValueDatetimeFunctions(FunctionContributions functionContributions) {
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_DATETIME_EQUALS, "(?1->>'value')::TIMESTAMPTZ = ?2::TIMESTAMPTZ")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_DATETIME_GREATER_THAN, "(?1->>'value')::TIMESTAMPTZ > ?2::TIMESTAMPTZ")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(
                JSON_VALUE_DATETIME_GREATER_THAN_EQUAL,
                "(?1->>'value')::TIMESTAMPTZ >= ?2::TIMESTAMPTZ"
            )
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(JSON_VALUE_DATETIME_LESS_THAN, "(?1->>'value')::TIMESTAMPTZ < ?2::TIMESTAMPTZ")
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
        functionRegistry
            .patternDescriptorBuilder(
                JSON_VALUE_DATETIME_LESS_THAN_EQUAL,
                "(?1->>'value')::TIMESTAMPTZ <= ?2::TIMESTAMPTZ"
            )
            .setInvariantType(
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
            )
            .setExactArgumentCount(2)
            .setArgumentListSignature("JSONB jsonb, STRING value")
            .register();
    }
}
