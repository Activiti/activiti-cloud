/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.identity.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.activiti.cloud.identity.config.Oauth2FeignConfiguration;
import org.activiti.cloud.identity.web.assembler.ModelRepresentationGroupAssembler;
import org.activiti.cloud.identity.web.assembler.ModelRepresentationUserAssembler;
import org.activiti.cloud.identity.web.controller.IdentityManagementController;
import org.springframework.context.annotation.Import;

/**
 * This annotation enables the IdentityManagementRestController that exposes
 * users and groups search using the target identity management service (e.g. Keycloak)
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import({Oauth2FeignConfiguration.class,
    IdentityManagementController.class,
    ModelRepresentationUserAssembler.class,
    ModelRepresentationGroupAssembler.class})
public @interface EnableIdentityManagementRestAPI {
}
