package org.activiti.cloud.services.modeling.rest.config;

import org.activiti.cloud.services.modeling.rest.controller.ModelController;
import org.activiti.cloud.services.modeling.rest.controller.ModelingRestExceptionHandler;
import org.activiti.cloud.services.modeling.rest.controller.ProjectController;
import org.activiti.cloud.services.modeling.rest.validation.ValidationControllerAdvice;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureBefore(ErrorMvcAutoConfiguration.class)
@Import({
    ModelController.class,
    ProjectController.class,
    ModelingRestExceptionHandler.class,
    ValidationControllerAdvice.class
})
public class RestControllerAutoConfiguration {

}
