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

package org.activiti.cloud.services.rest.controllers;

import org.activiti.cloud.services.rest.api.ServiceTaskAdminController;
import org.activiti.services.connectors.channel.IntegrationRequestReplayer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceTaskAdminControllerImpl implements ServiceTaskAdminController {

    private IntegrationRequestReplayer integrationRequestReplayer;

    public ServiceTaskAdminControllerImpl(IntegrationRequestReplayer integrationRequestReplayer) {
        this.integrationRequestReplayer = integrationRequestReplayer;
    }

    @Override
    public ResponseEntity<Void> replayServiceTask(String executionId, String flowNodeId) {
        integrationRequestReplayer.replay(executionId, flowNodeId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
