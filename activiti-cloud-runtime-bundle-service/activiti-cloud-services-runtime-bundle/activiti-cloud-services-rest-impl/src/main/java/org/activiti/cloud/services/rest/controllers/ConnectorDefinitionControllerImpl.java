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
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.rest.controllers;

import org.activiti.cloud.services.rest.api.ConnectorDefinitionController;
import org.activiti.cloud.services.rest.assemblers.ConnectorDefinitionResourceAssembler;
import org.activiti.cloud.services.rest.assemblers.ResourcesAssembler;
import org.activiti.core.common.model.connector.ConnectorDefinition;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ConnectorDefinitionControllerImpl implements ConnectorDefinitionController {

    private final List<ConnectorDefinition> connectorDefinitions;

    private final ConnectorDefinitionResourceAssembler connectorDefinitionResourceAssembler;

    private final ResourcesAssembler resourcesAssembler;

    public ConnectorDefinitionControllerImpl(
            List<ConnectorDefinition> connectorDefinitions,
            ConnectorDefinitionResourceAssembler connectorDefinitionResourceAssembler,
            ResourcesAssembler resourcesAssembler) {

        this.connectorDefinitions = connectorDefinitions;
        this.connectorDefinitionResourceAssembler = connectorDefinitionResourceAssembler;
        this.resourcesAssembler = resourcesAssembler;
    }

    @Override
    public Resources<Resource<ConnectorDefinition>> getConnectorDefinitions() {
        return resourcesAssembler.toResources(connectorDefinitions,
                connectorDefinitionResourceAssembler);
    }

    @Override
    public Resource<ConnectorDefinition> getConnectorDefinition(@PathVariable String id) {
        return connectorDefinitionResourceAssembler.toResource(connectorDefinitions.stream()
                .filter(connectorDefinition ->
                        connectorDefinition.getId().equals(id)).findAny().orElseThrow(() -> new ActivitiObjectNotFoundException(id + " not found")));
    }

}
