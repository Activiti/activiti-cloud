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

import java.time.Duration;
import org.activiti.cloud.services.query.app.count.SubscriberScopeRegistry;
import org.activiti.cloud.services.query.app.count.TaskCountRecomputer;
import org.activiti.cloud.services.query.app.repository.CustomizedJpaSpecificationExecutorImpl;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EnableJpaRepositories(
    basePackageClasses = ProcessInstanceRepository.class,
    repositoryBaseClass = CustomizedJpaSpecificationExecutorImpl.class
)
@EntityScan(basePackageClasses = ProcessInstanceEntity.class)
public class QueryRepositoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EntityFinder entityFinder() {
        return new EntityFinder();
    }

    /**
     * Declared here rather than in query-rest because both sides need the same instance: the REST tier
     * writes to it while serving counts, and the event consumer reads from it after commit. query-repo
     * is the only module both of them depend on.
     * <p>
     * The TTL must exceed the interval at which clients re-fetch their counts, or a subscriber can
     * expire out of the registry and stop receiving pushes - see {@link SubscriberScopeRegistry}.
     */
    @Bean
    @ConditionalOnMissingBean
    public SubscriberScopeRegistry subscriberScopeRegistry(
        @Value("${query.count-scopes.registry.ttl:PT15M}") Duration registryTtl,
        @Value("${query.count-scopes.registry.max-size:10000}") long registryMaxSize
    ) {
        return new SubscriberScopeRegistry(registryTtl, registryMaxSize);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskCountRecomputer taskCountRecomputer(
        TaskRepository taskRepository,
        TaskCandidateGroupRepository taskCandidateGroupRepository,
        TaskCandidateUserRepository taskCandidateUserRepository,
        SubscriberScopeRegistry subscriberScopeRegistry,
        @Value("${query.count-scopes.fan-out-warn-threshold:200}") int fanOutWarnThreshold
    ) {
        return new TaskCountRecomputer(
            taskRepository,
            taskCandidateGroupRepository,
            taskCandidateUserRepository,
            subscriberScopeRegistry,
            fanOutWarnThreshold
        );
    }
}
