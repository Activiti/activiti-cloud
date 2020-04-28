package org.activiti.cloud.connectors.starter.test.it;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile(ConnectorsITStreamHandlers.CONNECTOR_IT)
@Component
@EnableBinding({RuntimeMockStreams.class, MockCloudRuntimeEventsChannels.class})
public class ConnectorsITStreamHandlers {

    public static final String CONNECTOR_IT = "ConnectorIT";

    private static AtomicInteger integrationResultEventsCounter = new AtomicInteger();
    private static AtomicBoolean integrationErrorEventProduced = new AtomicBoolean();
    private static AtomicReference<IntegrationError> integrationErrorReference = new AtomicReference<>();

    private String integrationId;

    @StreamListener(value = RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
    public void consumeIntegrationResultsMock(IntegrationResult integrationResult) throws InterruptedException {
        assertThat(integrationResult.getIntegrationContext().getOutBoundVariables().get("var2")).isEqualTo(2);
        assertThat(integrationResult.getIntegrationContext().getId()).isEqualTo(getIntegrationId());
        integrationResultEventsCounter.incrementAndGet();
    }

    @StreamListener(value = RuntimeMockStreams.INTEGRATION_ERROR_CONSUMER)
    public void consumeIntegrationErrorMock(IntegrationError integrationError) throws InterruptedException {
        integrationErrorReference.set(integrationError);

        integrationErrorEventProduced.set(true);
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

    public AtomicBoolean isIntegrationErrorEventProduced() {
        return integrationErrorEventProduced;
    }

    public IntegrationError getIntegrationError() {
        return integrationErrorReference.get();
    }
}
