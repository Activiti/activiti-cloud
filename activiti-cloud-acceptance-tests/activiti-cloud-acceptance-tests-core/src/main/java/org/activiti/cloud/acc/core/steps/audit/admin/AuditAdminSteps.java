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
package org.activiti.cloud.acc.core.steps.audit.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.audit.admin.AuditAdminService;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

@EnableRuntimeFeignContext
public class AuditAdminSteps {

    @Autowired
    private AuditAdminService auditAdminService;

    @Autowired
    @Qualifier("auditBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public Collection<CloudRuntimeEvent> getEventsByEntityIdAdmin(String entityId) {
        String filter = "entityId:";
        return auditAdminService.getEvents(filter + entityId).getContent();
    }

    @Step
    public CollectionModel<EntityModel<CloudRuntimeEvent>> deleteEvents() {
        return auditAdminService.deleteEvents();
    }

    @Step
    public PagedModel<CloudRuntimeEvent> getEventsAdmin() {
        return auditAdminService.getEvents();
    }
}
