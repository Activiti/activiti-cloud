package org.activiti.cloud.conf;

import org.activiti.cloud.services.query.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.query.rest.CommonExceptionHandlerQuery;
import org.activiti.cloud.services.query.rest.ProcessDefinitionAdminController;
import org.activiti.cloud.services.query.rest.ProcessDefinitionController;
import org.activiti.cloud.services.query.rest.ProcessInstanceAdminController;
import org.activiti.cloud.services.query.rest.ProcessInstanceController;
import org.activiti.cloud.services.query.rest.ProcessInstanceDeleteController;
import org.activiti.cloud.services.query.rest.ProcessInstanceDiagramAdminController;
import org.activiti.cloud.services.query.rest.ProcessInstanceDiagramController;
import org.activiti.cloud.services.query.rest.ProcessInstanceTasksController;
import org.activiti.cloud.services.query.rest.ProcessInstanceVariableAdminController;
import org.activiti.cloud.services.query.rest.ProcessInstanceVariableController;
import org.activiti.cloud.services.query.rest.ProcessModelAdminController;
import org.activiti.cloud.services.query.rest.ProcessModelController;
import org.activiti.cloud.services.query.rest.TaskAdminController;
import org.activiti.cloud.services.query.rest.TaskController;
import org.activiti.cloud.services.query.rest.TaskDeleteController;
import org.activiti.cloud.services.query.rest.TaskVariableAdminController;
import org.activiti.cloud.services.query.rest.TaskVariableController;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    CommonExceptionHandlerQuery.class,
    ProcessDefinitionAdminController.class,
    ProcessDefinitionController.class,
    ProcessInstanceAdminController.class,
    ProcessInstanceController.class,
    ProcessInstanceDeleteController.class,
    ProcessInstanceDiagramAdminController.class,
    ProcessInstanceDiagramController.class,
    ProcessInstanceTasksController.class,
    ProcessInstanceVariableAdminController.class,
    ProcessInstanceVariableController.class,
    ProcessModelAdminController.class,
    ProcessModelController.class,
    TaskAdminController.class,
    TaskController.class,
    TaskDeleteController.class,
    TaskVariableAdminController.class,
    TaskVariableController.class
})
public class QueryRestControllersAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProcessDiagramGenerator processDiagramGenerator() {
        return new DefaultProcessDiagramGenerator();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessDiagramGeneratorWrapper processDiagramGeneratorWrapper(ProcessDiagramGenerator processDiagramGenerator) {
        return new ProcessDiagramGeneratorWrapper(processDiagramGenerator);
    }    

}
