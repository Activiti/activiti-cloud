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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;

@AutoConfiguration(before = FunctionRouterGatewayAutoConfiguration.class)
@ConditionalOnProperty(name = "function-router-gateway.metadata-store", havingValue = "caffeine")
@ConditionalOnClass(Caffeine.class)
public class CaffeineMetadataStoreAutoConfiguration {

    @Bean
    public Cache<String, String> functionRouterMetadataStoreCaffeineCache(
        @Value("${function-router-gateway.metadata-store.caffeine.size:10000}") Integer maximumSize,
        @Value("${function-router-gateway.metadata-store.caffeine.tll:PT1H}") Duration expireAfterWrite
    ) {
        return Caffeine.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(expireAfterWrite.toMillis(), TimeUnit.MILLISECONDS)
            .build();
    }

    @Bean
    public ConcurrentMetadataStore functionRouterMetadataStore(
        Cache<String, String> functionRouterMetadataStoreCaffeineCache
    ) {
        return new SimpleMetadataStore(functionRouterMetadataStoreCaffeineCache.asMap());
    }
}
