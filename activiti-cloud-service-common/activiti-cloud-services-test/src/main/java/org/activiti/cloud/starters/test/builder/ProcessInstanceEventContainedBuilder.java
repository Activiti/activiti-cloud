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
package org.activiti.cloud.starters.test.builder;

import java.util.Date;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.BPMNActivityImpl;
import org.activiti.api.runtime.model.impl.BPMNSequenceFlowImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.CloudProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNActivityStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCompletedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessSuspendedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudSequenceFlowTakenEventImpl;
import org.activiti.cloud.starters.test.EventsAggregator;

public class ProcessInstanceEventContainedBuilder {

    private final EventsAggregator eventsAggregator;

    public ProcessInstanceEventContainedBuilder(EventsAggregator eventsAggregator) {
        this.eventsAggregator = eventsAggregator;
    }

    public ProcessInstance aCompletedProcessInstance(String name) {
        ProcessInstance processInstance = buildProcessInstance(name);
        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(processInstance),
            new CloudProcessStartedEventImpl(processInstance, null, null),
            new CloudProcessCompletedEventImpl(processInstance)
        );
        return processInstance;
    }

    public ProcessInstanceImpl aRunningProcessInstance(String name) {
        ProcessInstanceImpl processInstance = buildProcessInstance(name);
        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(processInstance),
            new CloudProcessStartedEventImpl(processInstance, null, null)
        );
        return processInstance;
    }

    public ProcessInstanceImpl startSimpleProcessInstance(String processDefinitionId) {
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId(UUID.randomUUID().toString());
        process.setName("process");
        process.setProcessDefinitionKey("mySimpleProcess");
        process.setProcessDefinitionId(processDefinitionId);
        process.setProcessDefinitionVersion(1);

        BPMNActivityImpl startActivity = new BPMNActivityImpl("startEvent1", "", "startEvent");
        startActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        startActivity.setProcessInstanceId(process.getId());
        startActivity.setExecutionId(UUID.randomUUID().toString());

        BPMNSequenceFlowImpl sequenceFlow = new BPMNSequenceFlowImpl(
            "sid-68945AF1-396F-4B8A-B836-FC318F62313F",
            "startEvent1",
            "sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94"
        );
        sequenceFlow.setProcessDefinitionId(process.getProcessDefinitionId());
        sequenceFlow.setProcessInstanceId(process.getId());

        BPMNActivityImpl taskActivity = new BPMNActivityImpl(
            "sid-CDFE7219-4627-43E9-8CA8-866CC38EBA94",
            "Perform Action",
            "userTask"
        );
        taskActivity.setProcessDefinitionId(process.getProcessDefinitionId());
        taskActivity.setProcessInstanceId(process.getId());
        taskActivity.setExecutionId(UUID.randomUUID().toString());

        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(process),
            new CloudProcessStartedEventImpl(process, null, null),
            new CloudBPMNActivityStartedEventImpl(startActivity, processDefinitionId, process.getId()),
            new CloudBPMNActivityCompletedEventImpl(startActivity, processDefinitionId, process.getId()),
            new CloudSequenceFlowTakenEventImpl(sequenceFlow),
            new CloudBPMNActivityStartedEventImpl(taskActivity, processDefinitionId, process.getId())
        );

        return process;
    }

    private ProcessInstanceImpl buildProcessInstance(String name) {
        return buildProcessInstance(name, "testuser");
    }

    private ProcessInstanceImpl buildProcessInstance(String name, String initiator) {
        ProcessInstanceImpl completedProcess = new ProcessInstanceImpl();
        completedProcess.setId(UUID.randomUUID().toString());
        completedProcess.setInitiator(initiator);
        completedProcess.setName(name);
        completedProcess.setProcessDefinitionKey("my-proc");
        completedProcess.setProcessDefinitionId(UUID.randomUUID().toString());
        completedProcess.setProcessDefinitionName("my-proc-definition-name");
        return completedProcess;
    }

    private CloudProcessInstanceImpl buildSuspendedProcessInstance(String name) {
        CloudProcessInstanceImpl suspendedProcess = new CloudProcessInstanceImpl();
        suspendedProcess.setId(UUID.randomUUID().toString());
        suspendedProcess.setInitiator("testuser");
        suspendedProcess.setName(name);
        suspendedProcess.setProcessDefinitionKey("my-proc");
        suspendedProcess.setProcessDefinitionId(UUID.randomUUID().toString());
        suspendedProcess.setProcessDefinitionName("my-proc-definition-name");
        suspendedProcess.setStatus(ProcessInstance.ProcessInstanceStatus.SUSPENDED);
        return suspendedProcess;
    }

    public ProcessInstanceImpl aRunningProcessInstanceWithStartDate(String name, Date startDate) {
        ProcessInstanceImpl processInstance = buildProcessInstance(name);
        processInstance.setStartDate(startDate);
        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(processInstance),
            new CloudProcessStartedEventImpl(processInstance)
        );
        return processInstance;
    }

    public ProcessInstanceImpl aRunningProcessInstanceWithCompletedDate(String name, Date completedDate) {
        ProcessInstanceImpl completedProcess = buildProcessInstance(name);
        completedProcess.setCompletedDate(completedDate);
        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(completedProcess),
            new CloudProcessCompletedEventImpl(UUID.randomUUID().toString(), completedDate.getTime(), completedProcess)
        );
        return completedProcess;
    }

    public ProcessInstanceImpl aRunningProcessInstanceWithInitiator(String name, String initiator) {
        ProcessInstanceImpl processInstance = buildProcessInstance(name, initiator);
        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(processInstance),
            new CloudProcessStartedEventImpl(processInstance)
        );
        return processInstance;
    }

    public ProcessInstanceImpl aRunningProcessInstanceWithAppVersion(String name, String appVersion) {
        ProcessInstanceImpl processInstance = buildProcessInstance(name);
        processInstance.setAppVersion(appVersion);
        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(processInstance),
            new CloudProcessStartedEventImpl(processInstance)
        );
        return processInstance;
    }

    public CloudProcessInstanceImpl aRunningProcessInstanceWithSuspendedDate(String name, Date suspendedDate) {
        CloudProcessInstanceImpl suspendedProcess = buildSuspendedProcessInstance(name);
        suspendedProcess.setSuspendedDate(suspendedDate);
        eventsAggregator.addEvents(
            new CloudProcessCreatedEventImpl(suspendedProcess),
            new CloudProcessSuspendedEventImpl(UUID.randomUUID().toString(), suspendedDate.getTime(), suspendedProcess)
        );
        return suspendedProcess;
    }
}
