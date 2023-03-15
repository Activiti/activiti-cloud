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
package org.activiti.cloud.services.notifications.graphql.web.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GraphQLQueryResult {

    private Map<String, Object> data;
    private List<Map<String, Object>> errors;
    private Map<Object, Object> extensions;

    private GraphQLQueryResult(Builder builder) {
        this.data = builder.data;
        this.errors = builder.errors;
        this.extensions = builder.extensions;
    }

    /**
     * Default
     */
    GraphQLQueryResult() {}

    public Map<String, Object> getData() {
        return data;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

    public Map<Object, Object> getExtensions() {
        return extensions;
    }

    /**
     * Creates a builder to build {@link GraphQLQueryResult} and initialize it with the given object.
     * @param graphQLQueryResult to initialize the builder with
     * @return created builder
     */
    public static Builder builderFrom(GraphQLQueryResult graphQLQueryResult) {
        return new Builder(graphQLQueryResult);
    }

    /**
     * Builder to build {@link GraphQLQueryResult}.
     */
    public static final class Builder {

        private Map<String, Object> data = Collections.emptyMap();
        private List<Map<String, Object>> errors = Collections.emptyList();
        private Map<Object, Object> extensions = Collections.emptyMap();

        public Builder() {}

        private Builder(GraphQLQueryResult graphQLQueryResult) {
            this.data = graphQLQueryResult.data;
            this.errors = graphQLQueryResult.errors;
            this.extensions = graphQLQueryResult.extensions;
        }

        /**
         * Builder method for data parameter.
         * @param data field to set
         * @return builder
         */
        public Builder withData(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        /**
         * Builder method for errors parameter.
         * @param errors field to set
         * @return builder
         */
        public Builder withErrors(List<Map<String, Object>> errors) {
            this.errors = errors;
            return this;
        }

        /**
         * Builder method for extensions parameter.
         * @param extensions field to set
         * @return builder
         */
        public Builder withExtensions(Map<Object, Object> extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * Builder method of the builder.
         * @return built class
         */
        public GraphQLQueryResult build() {
            return new GraphQLQueryResult(this);
        }
    }
}
