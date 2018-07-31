package org.activiti.cloud.starter.tests.cmdendpoint;

import java.util.concurrent.atomic.AtomicBoolean;

import org.activiti.runtime.api.Result;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.payloads.ClaimTaskPayload;
import org.activiti.runtime.api.model.payloads.CompleteTaskPayload;
import org.activiti.runtime.api.model.payloads.ReleaseTaskPayload;
import org.activiti.runtime.api.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.runtime.api.model.payloads.ResumeProcessPayload;
import org.activiti.runtime.api.model.payloads.SetProcessVariablesPayload;
import org.activiti.runtime.api.model.payloads.SetTaskVariablesPayload;
import org.activiti.runtime.api.model.payloads.SignalPayload;
import org.activiti.runtime.api.model.payloads.StartProcessPayload;
import org.activiti.runtime.api.model.payloads.SuspendProcessPayload;
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
    private AtomicBoolean sendSignalAck = new AtomicBoolean(false);
    private AtomicBoolean setTaskVariablesAck = new AtomicBoolean(false);
    private AtomicBoolean setProcessVariablesAck = new AtomicBoolean(false);
    private AtomicBoolean removeProcessVariablesAck = new AtomicBoolean(false);

    @StreamListener(MessageClientStream.MY_CMD_RESULTS)
    public <T extends Result> void consumeStartProcessInstanceResults(Result result) {
        if (result.getPayload() instanceof StartProcessPayload) {
            assertThat(result.getEntity()).isNotNull();
            assertThat(result.getEntity()).isInstanceOf(ProcessInstance.class);
            assertThat(((ProcessInstance) result.getEntity()).getId()).isNotEmpty();
            processInstanceId = ((ProcessInstance) result.getEntity()).getId();
            startedProcessInstanceAck.set(true);
        } else if (result.getPayload() instanceof SuspendProcessPayload) {
            suspendedProcessInstanceAck.set(true);
        } else if (result.getPayload() instanceof ResumeProcessPayload) {
            activatedProcessInstanceAck.set(true);
        } else if (result.getPayload() instanceof ClaimTaskPayload) {
            claimedTaskAck.set(true);
        } else if (result.getPayload() instanceof ReleaseTaskPayload) {
            releasedTaskAck.set(true);
        } else if (result.getPayload() instanceof CompleteTaskPayload) {
            completedTaskAck.set(true);
        } else if (result.getPayload() instanceof SignalPayload) {
            sendSignalAck.set(true);
        } else if (result.getPayload() instanceof SetTaskVariablesPayload) {
            setTaskVariablesAck.set(true);
        } else if (result.getPayload() instanceof SetProcessVariablesPayload) {
            setProcessVariablesAck.set(true);
        } else if (result.getPayload() instanceof RemoveProcessVariablesPayload) {
            removeProcessVariablesAck.set(true);
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

    public AtomicBoolean getSendSignalAck() {
        return sendSignalAck;
    }

    public AtomicBoolean getSetTaskVariablesAck() {
        return setTaskVariablesAck;
    }

    public AtomicBoolean getSetProcessVariablesAck() {
        return setProcessVariablesAck;
    }

    public AtomicBoolean getRemoveProcessVariablesAck() {
        return removeProcessVariablesAck;
    }
}