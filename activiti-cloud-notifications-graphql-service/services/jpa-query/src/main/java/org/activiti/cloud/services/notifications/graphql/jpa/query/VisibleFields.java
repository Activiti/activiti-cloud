/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.notifications.graphql.jpa.query;

import graphql.Internal;
import graphql.com.google.common.collect.ImmutableList;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputFieldsContainer;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.visibility.GraphqlFieldVisibility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class VisibleFields implements GraphqlFieldVisibility {

    private final List<Pattern> patterns;

    /**
     * @param patterns the visible field patterns
     */
    @Internal
    private VisibleFields(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    @Override
    public List<GraphQLFieldDefinition> getFieldDefinitions(GraphQLFieldsContainer fieldsContainer) {
        return fieldsContainer
            .getFieldDefinitions()
            .stream()
            .filter(fieldDefinition -> !block(mkFQN(fieldsContainer.getName(), fieldDefinition.getName())))
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public GraphQLFieldDefinition getFieldDefinition(GraphQLFieldsContainer fieldsContainer, String fieldName) {
        GraphQLFieldDefinition fieldDefinition = fieldsContainer.getFieldDefinition(fieldName);
        if (fieldDefinition != null) {
            if (block(mkFQN(fieldsContainer.getName(), fieldDefinition.getName()))) {
                fieldDefinition = null;
            }
        }
        return fieldDefinition;
    }

    @Override
    public List<GraphQLInputObjectField> getFieldDefinitions(GraphQLInputFieldsContainer fieldsContainer) {
        return fieldsContainer
            .getFieldDefinitions()
            .stream()
            .filter(fieldDefinition -> !block(mkFQN(fieldsContainer.getName(), fieldDefinition.getName())))
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public GraphQLInputObjectField getFieldDefinition(GraphQLInputFieldsContainer fieldsContainer, String fieldName) {
        GraphQLInputObjectField fieldDefinition = fieldsContainer.getFieldDefinition(fieldName);
        if (fieldDefinition != null) {
            if (block(mkFQN(fieldsContainer.getName(), fieldDefinition.getName()))) {
                fieldDefinition = null;
            }
        }
        return fieldDefinition;
    }

    private boolean block(String fqn) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(fqn).matches()) {
                return false;
            }
        }
        return true;
    }

    private String mkFQN(String containerName, String fieldName) {
        return containerName + "." + fieldName;
    }

    public static VisibleFields.Builder newFieldsVisibility() {
        return new VisibleFields.Builder();
    }

    public static class Builder {

        private final List<Pattern> patterns = new ArrayList<>();

        public VisibleFields.Builder addPattern(String regexPattern) {
            return addCompiledPattern(Pattern.compile(regexPattern));
        }

        public VisibleFields.Builder addPatterns(Collection<String> regexPatterns) {
            regexPatterns.forEach(this::addPattern);
            return this;
        }

        public VisibleFields.Builder addCompiledPattern(Pattern regex) {
            patterns.add(regex);
            return this;
        }

        public VisibleFields.Builder addCompiledPatterns(Collection<Pattern> regexes) {
            regexes.forEach(this::addCompiledPattern);
            return this;
        }

        public GraphqlFieldVisibility build() {
            return new VisibleFields(patterns);
        }
    }
}
