package org.activiti.cloud.services.rest.conf;

import org.activiti.cloud.services.rest.controllers.CandidateGroupAdminControllerImpl;
import org.activiti.cloud.services.rest.controllers.CandidateGroupControllerImpl;
import org.activiti.cloud.services.rest.controllers.CandidateUserAdminControllerImpl;
import org.activiti.cloud.services.rest.controllers.CandidateUserControllerImpl;
import org.activiti.cloud.services.rest.controllers.ConnectorDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionAdminControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessDefinitionMetaControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceAdminControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceTasksControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableAdminControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceVariableControllerImpl;
import org.activiti.cloud.services.rest.controllers.RuntimeBundleExceptionHandler;
import org.activiti.cloud.services.rest.controllers.TaskAdminControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableAdminControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskVariableControllerImpl;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(ServicesRestWebMvcAutoConfiguration.class)
@Import({HomeControllerImpl.class,
        ConnectorDefinitionControllerImpl.class,
        ProcessDefinitionAdminControllerImpl.class,
        ProcessDefinitionControllerImpl.class,
        ProcessDefinitionMetaControllerImpl.class,
        ProcessInstanceAdminControllerImpl.class,
        ProcessInstanceControllerImpl.class,
        ProcessInstanceTasksControllerImpl.class,
        ProcessInstanceVariableAdminControllerImpl.class,
        ProcessInstanceVariableControllerImpl.class,
        RuntimeBundleExceptionHandler.class,
        TaskAdminControllerImpl.class,
        TaskControllerImpl.class,
        TaskVariableAdminControllerImpl.class,
        TaskVariableControllerImpl.class,
        CandidateUserControllerImpl.class,
        CandidateUserAdminControllerImpl.class,
        CandidateGroupControllerImpl.class,
        CandidateGroupAdminControllerImpl.class})
public class ServicesRestControllersAutoConfiguration {

}
