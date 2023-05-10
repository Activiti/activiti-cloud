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
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.activiti.QueryRestTestApplication;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeletedEventImpl;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.BPMNSequenceFlowRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QBPMNActivityEntity;
import org.activiti.cloud.services.query.model.QBPMNSequenceFlowEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;
import org.activiti.cloud.services.query.model.QServiceTaskEntity;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = QueryRestTestApplication.class)
@DirtiesContext
@AutoConfigureTestDatabase
public class ProcessDeletedEventHandlerTest {

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ServiceTaskRepository serviceTaskRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private BPMNActivityRepository bpmnActivityRepository;

    @Autowired
    private BPMNSequenceFlowRepository bpmnSequenceFlowRepository;

    @Autowired
    private EntityManager entityManager;

    private ProcessDeletedEventHandler handler;

    private String completedProcessId;

    private String runningProcessId;

    @BeforeEach
    @Transactional
    public void setUp() {
        handler = new ProcessDeletedEventHandler(entityManager);

        completedProcessId = UUID.randomUUID().toString();
        runningProcessId = UUID.randomUUID().toString();

        insertProcess(completedProcessId, ProcessInstanceStatus.COMPLETED);
        insertProcess(runningProcessId, ProcessInstanceStatus.RUNNING);

        assertThat(processInstanceRepository.existsById(completedProcessId)).isTrue();
        assertThat(taskRepository.exists(QTaskEntity.taskEntity.processInstanceId.eq(completedProcessId))).isTrue();
        assertThat(
            serviceTaskRepository.exists(QServiceTaskEntity.serviceTaskEntity.processInstanceId.eq(completedProcessId))
        )
            .isTrue();
        assertThat(
            variableRepository.exists(
                QProcessVariableEntity.processVariableEntity.processInstanceId.eq(completedProcessId)
            )
        )
            .isTrue();
        assertThat(
            bpmnActivityRepository.exists(
                QBPMNActivityEntity.bPMNActivityEntity.processInstanceId.eq(completedProcessId)
            )
        )
            .isTrue();
        assertThat(
            bpmnSequenceFlowRepository.exists(
                QBPMNSequenceFlowEntity.bPMNSequenceFlowEntity.processInstanceId.eq(completedProcessId)
            )
        )
            .isTrue();

        assertThat(processInstanceRepository.existsById(runningProcessId)).isTrue();
        assertThat(taskRepository.exists(QTaskEntity.taskEntity.processInstanceId.eq(runningProcessId))).isTrue();
        assertThat(
            serviceTaskRepository.exists(QServiceTaskEntity.serviceTaskEntity.processInstanceId.eq(runningProcessId))
        )
            .isTrue();
        assertThat(
            variableRepository.exists(
                QProcessVariableEntity.processVariableEntity.processInstanceId.eq(runningProcessId)
            )
        )
            .isTrue();
        assertThat(
            bpmnActivityRepository.exists(QBPMNActivityEntity.bPMNActivityEntity.processInstanceId.eq(runningProcessId))
        )
            .isTrue();
        assertThat(
            bpmnSequenceFlowRepository.exists(
                QBPMNSequenceFlowEntity.bPMNSequenceFlowEntity.processInstanceId.eq(runningProcessId)
            )
        )
            .isTrue();
    }

    @AfterEach
    public void cleanUp() {
        processInstanceRepository.deleteAll();
        taskRepository.deleteAll();
        serviceTaskRepository.deleteAll();
        variableRepository.deleteAll();
        bpmnActivityRepository.deleteAll();
        bpmnSequenceFlowRepository.deleteAll();
    }

    @Test
    @Transactional
    public void handleShouldDeleteCurrentProcessInstance() {
        //given
        ProcessInstanceImpl eventProcessInstance = new ProcessInstanceImpl();
        eventProcessInstance.setId(completedProcessId);
        CloudProcessDeletedEventImpl event = new CloudProcessDeletedEventImpl(eventProcessInstance);

        //when
        handler.handle(event);

        //then
        assertThat(processInstanceRepository.existsById(completedProcessId)).isFalse();
        assertThat(taskRepository.exists(QTaskEntity.taskEntity.processInstanceId.eq(completedProcessId))).isFalse();
        assertThat(
            serviceTaskRepository.exists(QServiceTaskEntity.serviceTaskEntity.processInstanceId.eq(completedProcessId))
        )
            .isFalse();
        assertThat(
            variableRepository.exists(
                QProcessVariableEntity.processVariableEntity.processInstanceId.eq(completedProcessId)
            )
        )
            .isFalse();
        assertThat(
            bpmnActivityRepository.exists(
                QBPMNActivityEntity.bPMNActivityEntity.processInstanceId.eq(completedProcessId)
            )
        )
            .isFalse();
        assertThat(
            bpmnSequenceFlowRepository.exists(
                QBPMNSequenceFlowEntity.bPMNSequenceFlowEntity.processInstanceId.eq(completedProcessId)
            )
        )
            .isFalse();

        assertThat(processInstanceRepository.existsById(runningProcessId)).isTrue();
        assertThat(taskRepository.exists(QTaskEntity.taskEntity.processInstanceId.eq(runningProcessId))).isTrue();
        assertThat(
            serviceTaskRepository.exists(QServiceTaskEntity.serviceTaskEntity.processInstanceId.eq(runningProcessId))
        )
            .isTrue();
        assertThat(
            variableRepository.exists(
                QProcessVariableEntity.processVariableEntity.processInstanceId.eq(runningProcessId)
            )
        )
            .isTrue();
        assertThat(
            bpmnActivityRepository.exists(QBPMNActivityEntity.bPMNActivityEntity.processInstanceId.eq(runningProcessId))
        )
            .isTrue();
        assertThat(
            bpmnSequenceFlowRepository.exists(
                QBPMNSequenceFlowEntity.bPMNSequenceFlowEntity.processInstanceId.eq(runningProcessId)
            )
        )
            .isTrue();
    }

    @Test
    public void handleShouldThrowExceptionWhenProcessInstanceIsNotCancelledOrCompleted() {
        //given
        ProcessInstanceImpl eventProcessInstance = new ProcessInstanceImpl();
        eventProcessInstance.setId(runningProcessId);
        CloudProcessDeletedEventImpl event = new CloudProcessDeletedEventImpl(eventProcessInstance);

        //then
        //when
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> handler.handle(event))
            .withMessage(
                handler.INVALID_PROCESS_INSTANCE_STATE,
                eventProcessInstance.getId(),
                ProcessInstanceStatus.RUNNING.name()
            );

        assertThat(processInstanceRepository.existsById(completedProcessId)).isTrue();
        assertThat(processInstanceRepository.existsById(runningProcessId)).isTrue();
    }

    @Test
    public void handleShouldThrowExceptionWhenRelatedProcessInstanceIsNotFound() {
        //given
        ProcessInstanceImpl eventProcessInstance = new ProcessInstanceImpl();
        eventProcessInstance.setId("404");
        CloudProcessDeletedEventImpl event = new CloudProcessDeletedEventImpl(eventProcessInstance);

        //then
        //when
        assertThatExceptionOfType(QueryException.class)
            .isThrownBy(() -> handler.handle(event))
            .withMessageContaining("Unable to find process instance with the given id: ");

        assertThat(processInstanceRepository.existsById(completedProcessId)).isTrue();
        assertThat(processInstanceRepository.existsById(runningProcessId)).isTrue();
    }

    @Test
    public void getHandledEventShouldReturnProcessDeletedEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessRuntimeEvent.ProcessEvents.PROCESS_DELETED.name());
    }

    private void insertProcess(String id, ProcessInstanceStatus status) {
        ProcessInstanceEntity processInstance = buildEntity(id, status);

        processInstance.getTasks().add(buildTaskEntity(UUID.randomUUID().toString(), id + "_Task_A", processInstance));
        processInstance.getTasks().add(buildTaskEntity(UUID.randomUUID().toString(), id + "_Task_B", processInstance));

        processInstance.getVariables().add(buildProcessVariableEntity(id + "_Var_A", processInstance));
        processInstance.getVariables().add(buildProcessVariableEntity(id + "_Var_B", processInstance));
        processInstance.getVariables().add(buildProcessVariableEntity(id + "_Var_C", processInstance));

        processInstance
            .getActivities()
            .add(
                buildBPMNActivityEntity(
                    UUID.randomUUID().toString(),
                    "MyApp",
                    "My_Test_Application",
                    "1",
                    "MyAppName",
                    "2",
                    processInstance
                )
            );

        processInstance
            .getServiceTasks()
            .add(
                buildServiceTaskEntity(
                    UUID.randomUUID().toString(),
                    "MyApp",
                    "My_Test_Application",
                    "1",
                    "MyAppName",
                    "2",
                    processInstance
                )
            );

        processInstance
            .getSequenceFlows()
            .add(
                buildBPMNSequenceFlowEntity(
                    UUID.randomUUID().toString(),
                    "MyApp",
                    "My_Test_Application",
                    "1",
                    "MyAppName",
                    "2",
                    processInstance
                )
            );

        processInstanceRepository.save(processInstance);
    }

    private ProcessInstanceEntity buildEntity(String id, ProcessInstanceStatus status) {
        ProcessInstanceEntity entity = new ProcessInstanceEntity();
        entity.setId(id);
        entity.setStatus(status);
        return entity;
    }

    private TaskEntity buildTaskEntity(String id, String name, ProcessInstanceEntity processInstance) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(id);
        taskEntity.setName(name);
        taskEntity.setProcessInstanceId(processInstance.getId());
        return taskRepository.save(taskEntity);
    }

    private ProcessVariableEntity buildProcessVariableEntity(String name, ProcessInstanceEntity processInstance) {
        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
        processVariableEntity.setName(name);
        processVariableEntity.setProcessInstanceId(processInstance.getId());
        return variableRepository.save(processVariableEntity);
    }

    private BPMNActivityEntity buildBPMNActivityEntity(
        String id,
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion,
        ProcessInstanceEntity processInstance
    ) {
        BPMNActivityEntity bpmnActivityEntity = new BPMNActivityEntity(
            serviceName,
            serviceFullName,
            serviceVersion,
            appName,
            appVersion
        );
        bpmnActivityEntity.setId(id);
        bpmnActivityEntity.setProcessInstanceId(processInstance.getId());
        return bpmnActivityRepository.save(bpmnActivityEntity);
    }

    private ServiceTaskEntity buildServiceTaskEntity(
        String id,
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion,
        ProcessInstanceEntity processInstance
    ) {
        ServiceTaskEntity serviceTaskEntity = new ServiceTaskEntity(
            serviceName,
            serviceFullName,
            serviceVersion,
            appName,
            appVersion
        );
        serviceTaskEntity.setId(id);
        serviceTaskEntity.setProcessInstanceId(processInstance.getId());
        serviceTaskEntity.setActivityType("serviceTask");
        return serviceTaskRepository.save(serviceTaskEntity);
    }

    private BPMNSequenceFlowEntity buildBPMNSequenceFlowEntity(
        String id,
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion,
        ProcessInstanceEntity processInstance
    ) {
        BPMNSequenceFlowEntity bpmnSequenceFlowEntity = new BPMNSequenceFlowEntity(
            serviceName,
            serviceFullName,
            serviceVersion,
            appName,
            appVersion
        );
        bpmnSequenceFlowEntity.setId(id);
        bpmnSequenceFlowEntity.setProcessInstanceId(processInstance.getId());
        return bpmnSequenceFlowRepository.save(bpmnSequenceFlowEntity);
    }
}
