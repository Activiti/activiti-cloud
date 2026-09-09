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
package org.activiti.cloud.services.query.app.count;

import org.activiti.cloud.services.query.app.repository.config.QueryRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * The smallest context that can run a real task count: a datasource, JPA, transactions, and the query
 * repository tier.
 * <p>
 * The four auto-configurations are listed explicitly rather than letting {@code @EnableAutoConfiguration}
 * loose, because this module also has Spring Cloud Stream and RabbitMQ on its classpath and a full
 * auto-configuration sweep would try to reach a broker that is not there.
 * <p>
 * {@link QueryRepositoryAutoConfiguration} is the one that matters: it carries the {@code @EntityScan} and
 * {@code @EnableJpaRepositories} for the query entities, and it declares the {@link SubscriberScopeRegistry}
 * and {@link TaskCountRecomputer} beans. That is the production wiring, not a test double - the POC gets the
 * same beans a running query service gets.
 * <p>
 * Deliberately <em>not</em> a Spring Boot test slice ({@code @DataJpaTest} and friends): those wrap each test
 * in a transaction that rolls back, and {@link TaskCountRecomputer} runs in {@code REQUIRES_NEW}, so it would
 * see none of the test's uncommitted rows. Counts must be read from committed state. That constraint is the
 * whole reason production recomputes <em>after</em> commit, so the POC honours it too.
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(
    {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        QueryRepositoryAutoConfiguration.class,
    }
)
class PushedTaskCountPocConfiguration {}
