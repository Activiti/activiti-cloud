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

import org.activiti.cloud.services.rest.api.RecoverController;
import org.activiti.cloud.services.rest.api.ReplayServiceTaskRequest;
import org.activiti.services.connectors.channel.IntegrationRequestReplayer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class RecoverControllerImpl implements RecoverController {

    private IntegrationRequestReplayer integrationRequestReplayer;

    public RecoverControllerImpl(IntegrationRequestReplayer integrationRequestReplayer) {
        this.integrationRequestReplayer = integrationRequestReplayer;
    }

    @Override
    public ResponseEntity<Void> replayServiceTask(@Valid @RequestBody ReplayServiceTaskRequest replayServiceTaskRequest) {
        integrationRequestReplayer.replay(replayServiceTaskRequest.getExecutionId(),
            replayServiceTaskRequest.getFlowNodeId());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
