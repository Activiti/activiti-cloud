package org.activiti.cloud.qa.helper;

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
            if(e.getProcessDefinitionKey().equals(processKey)){
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
