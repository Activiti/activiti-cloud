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
package org.activiti.cloud.services.query.app.repository.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class QueryTimeoutConfiguration {

    @Value("${spring.jpa.properties.hibernate.query.timeout:300000}")
    private int queryTimeout;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private int maxLifetime;

    @Bean
    public QueryTimeoutProperties queryTimeoutProperties() {
        return new QueryTimeoutProperties(queryTimeout, connectionTimeout, maxLifetime);
    }

    public static class QueryTimeoutProperties {

        private final int queryTimeout;

        private final int connectionTimeout;

        private final int maxLifetime;

        public QueryTimeoutProperties(int queryTimeout, int connectionTimeout, int maxLifetime) {
            this.queryTimeout = queryTimeout;
            this.connectionTimeout = connectionTimeout;
            this.maxLifetime = maxLifetime;
        }

        public int getQueryTimeout() {
            return queryTimeout;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public int getMaxLifetime() {
            return maxLifetime;
        }

        @Override
        public String toString() {
            return (
                "QueryTimeoutProperties{" +
                "queryTimeout=" +
                queryTimeout +
                ", connectionTimeout=" +
                connectionTimeout +
                ", maxLifetime=" +
                maxLifetime +
                '}'
            );
        }
    }
}
