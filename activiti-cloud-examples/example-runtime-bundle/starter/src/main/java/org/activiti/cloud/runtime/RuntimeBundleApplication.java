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
package org.activiti.cloud.runtime;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@ActivitiRuntimeBundle
@EnableScheduling
public class RuntimeBundleApplication {

    @Autowired
    private CacheManager cacheManager;

    public static void main(String[] args) {
        SpringApplication.run(RuntimeBundleApplication.class, args);
    }

    @Scheduled(fixedRate = 10000)
    void printCacheStats() {
        cacheManager
            .getCacheNames()
            .forEach(name -> {
                CaffeineCache cache = (CaffeineCache) cacheManager.getCache(name);
                CacheStats stats = cache.getNativeCache().stats();

                System.out.println("Cache: " + name);
                System.out.println("   Recording Stats: " + cache.getNativeCache().policy().isRecordingStats());
                System.out.println("   Estimated Size: " + cache.getNativeCache().estimatedSize());
                System.out.println("   Stats: " + stats);
            });
    }
}
