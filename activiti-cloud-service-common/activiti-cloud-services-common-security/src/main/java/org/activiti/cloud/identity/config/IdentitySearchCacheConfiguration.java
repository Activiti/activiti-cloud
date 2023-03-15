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
package org.activiti.cloud.identity.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class IdentitySearchCacheConfiguration {

    @Value("${identity.client.cache.cacheExpireAfterWrite:PT5m}")
    private String cacheExpireAfterWrite;

    @Value("${identity.client.cache.cacheMaxSize:1000}")
    private int cacheMaxSize;

    @Bean
    public CaffeineCache userSearchCache() {
        return new CaffeineCache(
            "userSearch",
            Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.parse(cacheExpireAfterWrite))
                .maximumSize(cacheMaxSize)
                .build()
        );
    }

    @Bean
    public CaffeineCache groupSearchCache() {
        return new CaffeineCache(
            "groupSearch",
            Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.parse(cacheExpireAfterWrite))
                .maximumSize(cacheMaxSize)
                .build()
        );
    }
}
