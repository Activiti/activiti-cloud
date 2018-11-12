package org.activiti.cloud.qa.assertions;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.activiti.api.model.shared.event.RuntimeEvent;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.process.model.events.SequenceFlowTakenEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.qa.rest.error.ExpectRestNotFound;
import org.jbehave.core.annotations.Then;
import steps.audit.AuditSteps;
import steps.query.ProcessQuerySteps;
import steps.runtime.ProcessRuntimeBundleSteps;

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
            //TODO: uncomment the SequenceFlowTakenEvent once the issue is solved
            Collection<CloudRuntimeEvent> events = auditSteps.getEventsByEntityId(processInstanceId);
            assertThat(events).isNotEmpty();
            assertThat(events)
                    .extracting(CloudRuntimeEvent::getEventType)
                    .containsExactlyInAnyOrder(
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            //SequenceFlowTakenEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED,
                            BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED,
                            ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED);
        });

        await().untilAsserted(() -> {
            ProcessInstance queryProcessInstance = processQuerySteps.getProcessInstance(processInstanceId);
            assertThat(queryProcessInstance).isNotNull();
            assertThat(queryProcessInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.COMPLETED);
        });
    }


}
