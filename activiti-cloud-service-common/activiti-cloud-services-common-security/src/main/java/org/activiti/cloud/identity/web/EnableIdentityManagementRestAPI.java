package org.activiti.cloud.identity.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.activiti.cloud.identity.web.assembler.ModelRepresentationGroupAssembler;
import org.activiti.cloud.identity.web.assembler.ModelRepresentationUserAssembler;
import org.activiti.cloud.identity.web.controller.IdentityManagementController;
import org.springframework.context.annotation.Import;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import({IdentityManagementController.class,
    ModelRepresentationUserAssembler.class,
    ModelRepresentationGroupAssembler.class})
public @interface EnableIdentityManagementRestAPI {
}
