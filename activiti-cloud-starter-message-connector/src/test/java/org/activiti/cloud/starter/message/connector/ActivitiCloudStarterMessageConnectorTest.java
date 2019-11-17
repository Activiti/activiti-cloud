package org.activiti.cloud.starter.message.connector;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.message.connector.MessageConnectorConsumer;
import org.activiti.cloud.services.message.connector.channels.MessageConnectorChannels;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ActivitiCloudStarterMessageConnectorTest {
    
    @Autowired
    private MessageConnectorChannels.Consumer consumer;

    @Autowired
    private MessageConnectorChannels.Producer producer;
    
    @Autowired
    private MessageConnectorConsumer messageConnectorConsumer;
    
    @SpringBootApplication
    static class Application {

    }
    
    @Test
    public void contextLoads() {
        assertThat(consumer).isNotNull();
        assertThat(producer).isNotNull();
        assertThat(messageConnectorConsumer).isNotNull();
    }
}
