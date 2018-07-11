package org.activiti.cloud.starter.tests.cmdendpoint;

import java.util.concurrent.atomic.AtomicBoolean;

import org.activiti.runtime.api.cmd.Command;
import org.activiti.runtime.api.cmd.SetProcessVariablesResult;
import org.activiti.runtime.api.cmd.result.ClaimTaskResult;
import org.activiti.runtime.api.cmd.result.CommandResult;
import org.activiti.runtime.api.cmd.result.CompleteTaskResult;
import org.activiti.runtime.api.cmd.result.ReleaseTaskResult;
import org.activiti.runtime.api.cmd.result.RemoveProcessVariablesResult;
import org.activiti.runtime.api.cmd.result.ResumeProcessResult;
import org.activiti.runtime.api.cmd.result.SendSignalResult;
import org.activiti.runtime.api.cmd.result.SetTaskVariablesResult;
import org.activiti.runtime.api.cmd.result.StartProcessResult;
import org.activiti.runtime.api.cmd.result.SuspendProcessResult;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

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
    public <T extends Command<?>>void consumeStartProcessInstanceResults(CommandResult<T> results) {
        if(results instanceof StartProcessResult){
            assertThat(((StartProcessResult)results).getProcessInstance()).isNotNull();
            assertThat(((StartProcessResult)results).getProcessInstance().getId()).isNotEmpty();
            processInstanceId = ((StartProcessResult)results).getProcessInstance().getId();
            startedProcessInstanceAck.set(true);
        }else if( results instanceof SuspendProcessResult){
            suspendedProcessInstanceAck.set(true);
        }else if(results instanceof ResumeProcessResult){
            activatedProcessInstanceAck.set(true);
        }else if(results instanceof ClaimTaskResult){
            claimedTaskAck.set(true);
        }else if(results instanceof ReleaseTaskResult){
            releasedTaskAck.set(true);
        }else if(results instanceof CompleteTaskResult){
            completedTaskAck.set(true);
        } else if (results instanceof SendSignalResult) {
            sendSignalAck.set(true);
        } else if (results instanceof SetTaskVariablesResult) {
            setTaskVariablesAck.set(true);
        } else if (results instanceof SetProcessVariablesResult) {
            setProcessVariablesAck.set(true);
        } else if (results instanceof RemoveProcessVariablesResult) {
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