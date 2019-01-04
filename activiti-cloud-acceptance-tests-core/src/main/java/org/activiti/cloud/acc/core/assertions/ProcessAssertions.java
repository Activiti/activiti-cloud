package org.activiti.cloud.acc.core.assertions;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.process.model.events.BPMNSequenceFlowTakenEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.jbehave.core.annotations.Then;
import org.activiti.cloud.acc.core.steps.audit.AuditSteps;
import org.activiti.cloud.acc.core.steps.query.ProcessQuerySteps;
import org.activiti.cloud.acc.core.steps.runtime.ProcessRuntimeBundleSteps;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

public class ProcessAssertions {

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;

    @Steps
    private AuditSteps auditSteps;

    @Then("the process is completed")
    public void assertThatProcessCompleted() throws Exception{

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        assertThatThrownBy(() -> {
            processRuntimeBundleSteps.getProcessInstanceById(processInstanceId);
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("Unable to find process instance for the given id");

        await().untilAsserted(() -> {
            ProcessInstance queryProcessInstance = processQuerySteps.getProcessInstance(processInstanceId);
            assertThat(queryProcessInstance).isNotNull();
            assertThat(queryProcessInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.COMPLETED);
        });
    }

    @Then("the process instance information is correctly propagated")
    public void assertThatInformationIsPropagated() throws Exception{

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            ProcessInstance queryProcessInstance = processQuerySteps.getProcessInstance(processInstanceId);
            assertThat(queryProcessInstance.getBusinessKey()).isEqualTo("businessKey");
            assertThat(queryProcessInstance.getName()).isEqualTo("processInstanceName");
        });
    }

    @Then("the events are as expected for Process Information")
    public void assertThatEventsAreAsExpectedForProcessInformation() throws Exception {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByEntityId(processInstanceId);
            assertThat(events).isNotEmpty();
            assertThat(events)
                    .extracting(CloudRuntimeEvent::getEventType)
                    .containsExactlyInAnyOrder(
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            BPMNSequenceFlowTakenEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED);
        });
    }

    @Then("the events are as expected for Process Information with variables")
    public void assertThatEventsAreAsExpectedForProcessInformationWithVariables() throws Exception {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByEntityId(processInstanceId);
            assertThat(events).isNotEmpty();
            assertThat(events)
                    .extracting(CloudRuntimeEvent::getEventType)
                    .containsExactlyInAnyOrder(
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED,
                            VariableEvent.VariableEvents.VARIABLE_CREATED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            BPMNSequenceFlowTakenEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED);
        });
    }

    @Then("the events are as expected for Process with Generic BPMN Task")
    public void assertThatEventsAreAsExpectedForProcessWithGenericBPMNTask() throws Exception {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByEntityId(processInstanceId);
            assertThat(events).isNotEmpty();
            assertThat(events)
                    .extracting(CloudRuntimeEvent::getEventType)
                    .containsExactlyInAnyOrder(
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            BPMNSequenceFlowTakenEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            BPMNSequenceFlowTakenEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED);
        });
    }

    @Then("the events are as expected for UserTask with no User or Group Assignment")
    public void assertThatEventsAreAsExpectedForProcessWithUserTaskWithNoAssignments() throws Exception {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByEntityId(processInstanceId);
            assertThat(events).isNotEmpty();
            assertThat(events)
                    .extracting(CloudRuntimeEvent::getEventType)
                    .containsExactlyInAnyOrder(
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            BPMNSequenceFlowTakenEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED);
        });
    }

    @Then("the events are as expected for the deletion of UserTask with no User or Group Assignment")
    public void assertThatEventsAreAsExpectedForProcessWithUserTaskWithNoAssignmentsWhenDeleted() throws Exception {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByEntityId(processInstanceId);
            assertThat(events).isNotEmpty();
            assertThat(events)
                    .extracting(CloudRuntimeEvent::getEventType)
                    .containsExactlyInAnyOrder(
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            BPMNSequenceFlowTakenEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED);
        });
    }

    @Then("the process cannot be found")
    public void assertThatTheProcessCannotBeFound(){

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        assertThatThrownBy(() -> {
            processRuntimeBundleSteps.getProcessInstanceById(processInstanceId);
        }).isInstanceOf(Exception.class)
                .hasMessageContaining("Unable to find process instance for the given id");
    }

    @Then("the events are as expected for the suspension of UserTask with no User or Group Assignment")
    public void assertThatEventsAreAsExpectedForProcessWithUserTaskWithNoAssignmentsWhenSuspended() {

        String processInstanceId = Serenity.sessionVariableCalled("processInstanceId");

        await().untilAsserted(() -> {
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByEntityId(processInstanceId);
            assertThat(events).isNotEmpty();
            assertThat(events)
                    .extracting(CloudRuntimeEvent::getEventType)
                    .contains(
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_SUSPENDED);
        });
    }








}
