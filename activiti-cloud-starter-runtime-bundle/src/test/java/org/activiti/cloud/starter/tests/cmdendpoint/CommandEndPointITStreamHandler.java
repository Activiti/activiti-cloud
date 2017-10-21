package org.activiti.cloud.starter.tests.cmdendpoint;

import java.util.concurrent.atomic.AtomicBoolean;

import org.activiti.cloud.services.core.model.commands.results.AbstractCommandResults;
import org.activiti.cloud.services.core.model.commands.results.ActivateProcessInstanceResults;
import org.activiti.cloud.services.core.model.commands.results.ClaimTaskResults;
import org.activiti.cloud.services.core.model.commands.results.CompleteTaskResults;
import org.activiti.cloud.services.core.model.commands.results.ReleaseTaskResults;
import org.activiti.cloud.services.core.model.commands.results.StartProcessInstanceResults;
import org.activiti.cloud.services.core.model.commands.results.SuspendProcessInstanceResults;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.*;

@Profile(CommandEndPointITStreamHandler.COMMAND_ENDPOINT_IT)
@Component
@EnableBinding(MessageClientStream.class)
public class CommandEndPointITStreamHandler {

    public static final String COMMAND_ENDPOINT_IT = "CommandEndpointIT";

    private String processInstanceId;

    private AtomicBoolean startedProcessInstanceAck = new AtomicBoolean(false);
    private AtomicBoolean suspendedProcessInstanceAck = new AtomicBoolean(false);
    private AtomicBoolean activatedProcessInstanceAck = new AtomicBoolean(false);
    private AtomicBoolean claimedTaskAck = new AtomicBoolean(false);
    private AtomicBoolean releasedTaskAck = new AtomicBoolean(false);
    private AtomicBoolean completedTaskAck = new AtomicBoolean(false);

    @StreamListener(MessageClientStream.MY_CMD_RESULTS)
    public void consumeResults(AbstractCommandResults results) {
        assertThat(results).isNotNull();
        if (results instanceof StartProcessInstanceResults) {
            assertThat(((StartProcessInstanceResults) results).getProcessInstance()).isNotNull();
            assertThat(((StartProcessInstanceResults) results).getProcessInstance().getId()).isNotEmpty();
            processInstanceId = ((StartProcessInstanceResults) results).getProcessInstance().getId();
            startedProcessInstanceAck.set(true);
        } else if (results instanceof SuspendProcessInstanceResults) {
            suspendedProcessInstanceAck.set(true);
        } else if (results instanceof ActivateProcessInstanceResults) {
            activatedProcessInstanceAck.set(true);
        } else if (results instanceof ClaimTaskResults) {
            claimedTaskAck.set(true);
        } else if (results instanceof ReleaseTaskResults) {
            releasedTaskAck.set(true);
        } else if (results instanceof CompleteTaskResults) {
            completedTaskAck.set(true);
        }
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public AtomicBoolean getStartedProcessInstanceAck() {
        return startedProcessInstanceAck;
    }

    public AtomicBoolean getSuspendedProcessInstanceAck() {
        return suspendedProcessInstanceAck;
    }

    public AtomicBoolean getActivatedProcessInstanceAck() {
        return activatedProcessInstanceAck;
    }

    public AtomicBoolean getClaimedTaskAck() {
        return claimedTaskAck;
    }

    public AtomicBoolean getReleasedTaskAck() {
        return releasedTaskAck;
    }

    public AtomicBoolean getCompletedTaskAck() {
        return completedTaskAck;
    }
}