/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.notifications.graphql.events.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.activiti.cloud.services.notifications.graphql.events.SpELTemplateRoutingKey;

@SpELTemplateRoutingKey("engineEvents.#{['serviceName']?:'_'}.#{['appName']?:'_'}.#{['eventType']?:'_'}.#{['processDefinitionKey']?:'_'}.#{['processInstanceId']?:'_'}.#{['businessKey']?:'_'}")
public class EngineEvent extends LinkedHashMap<String, Object>{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    public EngineEvent() {
        super();
    }

    public EngineEvent(Map<? extends String, ? extends Object> m) {
        super(m);
    }
    
    public String getEventType() {
        return get("eventType").toString();
    }

}
