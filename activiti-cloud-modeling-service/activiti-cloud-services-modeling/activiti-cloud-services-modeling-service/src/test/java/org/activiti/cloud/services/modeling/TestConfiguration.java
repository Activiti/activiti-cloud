package org.activiti.cloud.services.modeling;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
