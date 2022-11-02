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
package org.activiti.cloud.services.events.services;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.shared.NotFoundException;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.CloudProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.RuntimeBundleMessageBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudProcessDeletedService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CloudProcessDeletedService.class);

  private final Set<ProcessInstanceStatus> deleteStatuses = Set.of(ProcessInstanceStatus.COMPLETED, ProcessInstanceStatus.CANCELLED);

  private final String DELETE_PROCESS_NOT_ALLOWED = "Process Instance %s is not in status: " +
      String.join(", ", deleteStatuses.stream().map(Enum::name).collect(Collectors.toList()));

  private final ProcessEngineChannels producer;

  private final ProcessAdminRuntime processAdminRuntime;
  private final RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory;
  private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;


  public CloudProcessDeletedService(ProcessEngineChannels producer,
      ProcessAdminRuntime processAdminRuntime,
      RuntimeBundleMessageBuilderFactory runtimeBundleMessageBuilderFactory,
      RuntimeBundleInfoAppender runtimeBundleInfoAppender) {

    this.producer = producer;
    this.processAdminRuntime = processAdminRuntime;
    this.runtimeBundleMessageBuilderFactory = runtimeBundleMessageBuilderFactory;
    this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
  }

  public void sendDeleteEvent(String processInstanceId) {
    try {
      ProcessInstance processInstance = processAdminRuntime.processInstance(processInstanceId);
      if(processInstance != null && !deleteStatuses.contains(processInstance.getStatus())){
        throw new IllegalStateException(String.format(DELETE_PROCESS_NOT_ALLOWED, processInstanceId));
      }
    } catch(NotFoundException e){
      LOGGER.debug("Process Instance " + processInstanceId + " not found. Sending PROCESS_DELETE event.");
    }

    sendEvent(buildProcessInstance(processInstanceId));
  }

  protected void sendEvent(ProcessInstance processInstance) {

    producer.auditProducer().send(
        runtimeBundleMessageBuilderFactory.create()
            .withPayload(buildEvents(processInstance))
            .build()
    );
  }

  protected List<CloudRuntimeEvent<?, ?>> buildEvents(ProcessInstance processInstance) {
    CloudProcessDeletedEventImpl event = new CloudProcessDeletedEventImpl(processInstance);
    return Arrays.asList(runtimeBundleInfoAppender.appendRuntimeBundleInfoTo(event));
  }

  protected ProcessInstance buildProcessInstance(String processInstanceId){
    CloudProcessInstanceImpl processInstance = new CloudProcessInstanceImpl();
    processInstance.setId(processInstanceId);
    return processInstance;
  }

}
