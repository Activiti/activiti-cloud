package org.activiti.cloud.services.modeling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.RequestHandlerSelectors;

import java.util.function.Predicate;

@Configuration
public class SwaggerConfiguration {
    @Bean
    public Predicate<RequestHandler> apiSelector() {
        return RequestHandlerSelectors.basePackage("org.activiti.cloud.services.modeling.rest.controller")::apply;
    }
}
