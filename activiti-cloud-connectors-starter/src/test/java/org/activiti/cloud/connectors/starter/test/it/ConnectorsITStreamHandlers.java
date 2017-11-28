package org.activiti.cloud.connectors.starter.test.it;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.activiti.cloud.connectors.starter.model.IntegrationResultEvent;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Profile(ConnectorsITStreamHandlers.CONNECTOR_IT)
@Component
@EnableBinding({RuntimeMockStreams.class, MockProcessEngineChannels.class})
public class ConnectorsITStreamHandlers {

    public static final String CONNECTOR_IT = "ConnectorIT";

    private static AtomicInteger integrationResultEventsCounter = new AtomicInteger();
    private static AtomicBoolean startProcessInstanceCmdArrived = new AtomicBoolean(false);
    private String executionId;

    @StreamListener(value = RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
    public void consumeIntegrationResultsMock(IntegrationResultEvent integrationResultEvent) throws InterruptedException {
        assertThat(integrationResultEvent.getVariables().get("var2")).isEqualTo(2);
        assertThat(integrationResultEvent.getExecutionId()).isEqualTo(getExecutionId());
        integrationResultEventsCounter.incrementAndGet();
    }

    @StreamListener(value = MockProcessEngineChannels.COMMAND_CONSUMER)
    public void consumeProcessRuntimeCmd(StartProcessInstanceCmd startProcessInstanceCmd) throws InterruptedException {

        assertThat(startProcessInstanceCmd.getVariables().get("var2")).isEqualTo(2);
        assertThat(startProcessInstanceCmd.getProcessDefinitionId()).isEqualTo("MyOtherProcessDef");

        startProcessInstanceCmdArrived.set(true);
    }

    public AtomicInteger getIntegrationResultEventsCounter() {
        return integrationResultEventsCounter;
    }

    public AtomicBoolean isStartProcessInstanceCmdArrived() {
        return startProcessInstanceCmdArrived;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
}
