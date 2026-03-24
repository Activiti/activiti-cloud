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
package org.activiti.cloud.starter.tests.conf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * Supplies a {@link LockRegistry} for integration tests: {@link
 * org.activiti.services.connectors.channel.ServiceTaskIntegrationResultEventHandler} requires one and
 * no production auto-configuration always provides it in the test slice.
 */
@Configuration
public class ITLockRegistryConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockRegistry.class)
    public LockRegistry lockRegistry() {
        return new DefaultLockRegistry();
    }
}
