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

package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.identity.IdentityService;
import org.activiti.cloud.identity.model.User;
import org.springframework.util.StringUtils;

public class AuditServiceInfoAppender {

    private final IdentityService identityService;

    public AuditServiceInfoAppender(IdentityService identityService) {
        this.identityService = identityService;
    }

    public CloudRuntimeEventImpl<?, ?> appendAuditServiceInfoTo(
        CloudRuntimeEventImpl<?, ?> cloudRuntimeEvent,
        String actor
    ) {
        if (StringUtils.hasText(actor)) {
            User actorUser = this.identityService.findUserByName(actor);
            cloudRuntimeEvent.setActor(actorUser.getId());
        }

        return cloudRuntimeEvent;
    }
}
