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

package org.activiti.cloud.acc.core.helper;

import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.hateoas.PagedResources;

import java.util.ArrayList;
import java.util.Collection;

public class Filters {

    public static Collection<CloudProcessInstance> checkProcessInstances(PagedResources<CloudProcessInstance> resource, String processKey){
        Collection<CloudProcessInstance> rawCollection = resource.getContent();
        Collection<CloudProcessInstance> filteredCollection = new ArrayList<>();
        for(CloudProcessInstance e : rawCollection){
            if(e.getProcessDefinitionKey().equals(processKey) ){
                filteredCollection.add(e);
            }
        }
        return filteredCollection;
    }

    public static Collection<CloudRuntimeEvent> checkEvents(Collection<CloudRuntimeEvent> rawCollection, String processKey){

        Collection<CloudRuntimeEvent> filteredCollection = new ArrayList<>();
        for(CloudRuntimeEvent e : rawCollection){
            Object element = e.getEntity();


            if( element instanceof ProcessInstanceImpl && ((((ProcessInstanceImpl) element).getProcessDefinitionKey() != null))){
                if((((ProcessInstanceImpl) element).getProcessDefinitionKey().equals(processKey))) {
                    filteredCollection.add(e);
                }
            }
        }
        return filteredCollection;
    }
}
