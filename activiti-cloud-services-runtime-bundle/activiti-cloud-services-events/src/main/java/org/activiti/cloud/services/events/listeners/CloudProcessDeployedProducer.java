/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.events.listeners;

import java.util.List;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.RepositoryService;
import org.activiti.runtime.api.model.impl.APIProcessDefinitionConverter;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.support.MessageBuilder;

public class CloudProcessDeployedProducer implements ApplicationListener<ApplicationReadyEvent> {

    private RepositoryService repositoryService;
    private APIProcessDefinitionConverter processDefinitionConverter;
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;
    private ProcessEngineChannels producer;

    public CloudProcessDeployedProducer(RepositoryService repositoryService,
                                        APIProcessDefinitionConverter processDefinitionConverter,
                                        RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                        ProcessEngineChannels producer) {
        this.repositoryService = repositoryService;
        this.processDefinitionConverter = processDefinitionConverter;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
        this.producer = producer;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!WebApplicationType.NONE.equals(event.getSpringApplication().getWebApplicationType())) {
            List<ProcessDefinition> processDefinitions = processDefinitionConverter.from(repositoryService.createProcessDefinitionQuery().list());
            producer.auditProducer().send(
                    MessageBuilder
                            .withPayload(
                                    processDefinitions
                                            .stream()
                                            .map(definition -> {
                                                CloudProcessDeployedEventImpl deployedEvent = new CloudProcessDeployedEventImpl(definition);
                                                runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(deployedEvent);
                                                return deployedEvent;
                                            })
                                            .toArray(CloudRuntimeEvent<?, ?>[]::new))
                            .build());
        }
    }
}
