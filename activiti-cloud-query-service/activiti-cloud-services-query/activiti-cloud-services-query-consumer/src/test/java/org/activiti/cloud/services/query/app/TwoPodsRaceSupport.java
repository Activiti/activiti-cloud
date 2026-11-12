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
package org.activiti.cloud.services.query.app;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.conf.DefaultConsumerPartitionedChannelKeySelector;
import org.activiti.cloud.services.query.events.handlers.BPMNActivityCancelledEventHandler;
import org.activiti.cloud.services.query.events.handlers.BPMNActivityCompletedEventHandler;
import org.activiti.cloud.services.query.events.handlers.BPMNActivityStartedEventHandler;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ProcessCandidateStarterGroupEntity;
import org.activiti.cloud.services.query.model.ProcessCandidateStarterUserEntity;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.integration.channel.PartitionedChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

final class TwoPodsRaceSupport {

    private TwoPodsRaceSupport() {}

    static StandardServiceRegistry registry(PostgreSQLContainer postgres, String hbm2ddlAuto) {
        return new StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.url", postgres.getJdbcUrl())
            .applySetting("hibernate.connection.username", postgres.getUsername())
            .applySetting("hibernate.connection.password", postgres.getPassword())
            .applySetting("hibernate.connection.driver_class", "org.postgresql.Driver")
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.hbm2ddl.auto", hbm2ddlAuto)
            .applySetting(
                "hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
            )
            .build();
    }

    static SessionFactory buildSessionFactory(StandardServiceRegistry registry) {
        MetadataSources sources = new MetadataSources(registry)
            .addAnnotatedClass(ApplicationEntity.class)
            .addAnnotatedClass(BPMNActivityEntity.class)
            .addAnnotatedClass(BPMNSequenceFlowEntity.class)
            .addAnnotatedClass(IntegrationContextEntity.class)
            .addAnnotatedClass(ProcessCandidateStarterGroupEntity.class)
            .addAnnotatedClass(ProcessCandidateStarterUserEntity.class)
            .addAnnotatedClass(ProcessDefinitionEntity.class)
            .addAnnotatedClass(ProcessInstanceEntity.class)
            .addAnnotatedClass(ProcessInstanceHierarchyEntity.class)
            .addAnnotatedClass(ProcessModelEntity.class)
            .addAnnotatedClass(ProcessVariableEntity.class)
            .addAnnotatedClass(ServiceTaskEntity.class)
            .addAnnotatedClass(TaskCandidateGroupEntity.class)
            .addAnnotatedClass(TaskCandidateUserEntity.class)
            .addAnnotatedClass(TaskEntity.class)
            .addAnnotatedClass(TaskVariableEntity.class);
        return sources.buildMetadata().buildSessionFactory();
    }

    static final class Pod {

        private final TransactionTemplate transactionTemplate;
        private final PartitionedChannel inputChannel;
        private final CompletableFuture<Outcome> outcome = new CompletableFuture<>();

        Pod(EntityManagerFactory entityManagerFactory) {
            EntityManager entityManager = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);

            JpaTransactionManager transactionManager = new JpaTransactionManager(entityManagerFactory);
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            QueryEventHandlerContext handlerContext = new QueryEventHandlerContext(
                Set.of(
                    new BPMNActivityStartedEventHandler(entityManager, transactionManager),
                    new BPMNActivityCompletedEventHandler(entityManager, transactionManager),
                    new BPMNActivityCancelledEventHandler(entityManager, transactionManager)
                )
            );
            QueryEventHandlerContextOptimizer optimizer = new QueryEventHandlerContextOptimizer(entityManager);
            QueryConsumerMessageHandler messageHandler = new QueryConsumerMessageHandler(
                handlerContext,
                optimizer,
                entityManager,
                new QueueChannel()
            ).chunkSize(100);

            this.inputChannel = MessageChannels.partitioned(4)
                .partitionKey(new DefaultConsumerPartitionedChannelKeySelector())
                .workerQueueSize(10)
                .getObject();

            this.inputChannel.subscribe(message -> {
                try {
                    @SuppressWarnings("unchecked")
                    List<CloudRuntimeEvent<?, ?>> events = (List<CloudRuntimeEvent<?, ?>>) message.getPayload();
                    Message<List<CloudRuntimeEvent<?, ?>>> forwarded = MessageBuilder.withPayload(events)
                        .copyHeaders(message.getHeaders())
                        .build();

                    transactionTemplate.executeWithoutResult(status -> messageHandler.accept(forwarded));
                    outcome.complete(Outcome.ok());
                } catch (Exception ex) {
                    outcome.complete(Outcome.failure(ex));
                }
            });
        }

        void send(Message<List<CloudRuntimeEvent<?, ?>>> message) {
            inputChannel.send(message);
        }

        Outcome awaitOutcome() throws Exception {
            return outcome.get(15, TimeUnit.SECONDS);
        }
    }

    record Outcome(boolean committed, Exception exception) {
        static Outcome ok() {
            return new Outcome(true, null);
        }

        static Outcome failure(Exception exception) {
            return new Outcome(false, exception);
        }
    }
}
