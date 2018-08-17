package org.activiti.cloud.connectors.starter.test.it;

import java.util.concurrent.atomic.AtomicInteger;

import org.activiti.cloud.api.process.model.IntegrationResult;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Profile(ConnectorsITStreamHandlers.CONNECTOR_IT)
@Component
@EnableBinding({RuntimeMockStreams.class, MockCloudRuntimeEventsChannels.class})
public class ConnectorsITStreamHandlers {

    public static final String CONNECTOR_IT = "ConnectorIT";

    private static AtomicInteger integrationResultEventsCounter = new AtomicInteger();
    private String integrationId;

    @StreamListener(value = RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
    public void consumeIntegrationResultsMock(IntegrationResult integrationResult) throws InterruptedException {
        assertThat(integrationResult.getIntegrationContext().getOutBoundVariables().get("var2")).isEqualTo(2);
        assertThat(integrationResult.getIntegrationContext().getId()).isEqualTo(getIntegrationId());
        integrationResultEventsCounter.incrementAndGet();
    }


    public AtomicInteger getIntegrationResultEventsCounter() {
        return integrationResultEventsCounter;
    }

    public String getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
    }
}
