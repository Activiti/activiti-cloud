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
package org.activiti.cloud.common.messaging.spring.integration;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

public class RedisKeyValueMetadataStore implements ConcurrentMetadataStore {

    private final StringRedisTemplate redisTemplate;
    private final String prefix;
    private final Duration ttl;

    public RedisKeyValueMetadataStore(StringRedisTemplate redisTemplate, String prefix, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.prefix = prefix;
        this.ttl = ttl;
    }

    private String opsKey(String key) {
        return this.prefix + ":" + key;
    }

    @Override
    public void put(String key, String value) {
        // Saves as a plain string key and enforces the TTL instantly
        this.redisTemplate.opsForValue().set(opsKey(key), value, this.ttl);
    }

    @Override
    public String get(String key) {
        return this.redisTemplate.opsForValue().get(opsKey(key));
    }

    @Override
    public String putIfAbsent(String key, String value) {
        String actualKey = opsKey(key);
        Boolean success = this.redisTemplate.opsForValue().setIfAbsent(actualKey, value, this.ttl);
        if (Boolean.TRUE.equals(success)) {
            return null; // Spring metadata store contract expects null if inserted successfully
        }
        return this.redisTemplate.opsForValue().get(actualKey);
    }

    @Override
    public String remove(String key) {
        String actualKey = opsKey(key);
        return this.redisTemplate.opsForValue().getAndDelete(actualKey);
    }

    @Override
    public boolean replace(String key, String oldValue, String newValue) {
        // Fallback placeholder logic for conditional updates
        String currentValue = this.redisTemplate.opsForValue().getAndSet(opsKey(key), newValue);

        return currentValue != null;
    }
}
