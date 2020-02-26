package org.activiti.cloud.services.messages.test.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.messages.core.channels.MessageConnectorProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MessageCoreAutoConfigurationTest {
    
    @Autowired
    private MessageConnectorProcessor messageConnectorProcessor;

    @Autowired
    private IntegrationFlow messageConnectorIntegrationFlow;
    
    @SpringBootApplication
    static class Application {
        
    }
    
    @Test
    public void contextLoads() {
        assertThat(messageConnectorProcessor).isNotNull();
        assertThat(messageConnectorIntegrationFlow).isNotNull();
    }
}
