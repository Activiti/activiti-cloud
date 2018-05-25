package org.activiti.cloud.services.core;

import org.activiti.cloud.services.events.listeners.MessageProducerActivitiEventListener;
import org.activiti.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessEngineWrapper {

    @Autowired
    public ProcessEngineWrapper(RuntimeService runtimeService,
                                MessageProducerActivitiEventListener listener) {
        runtimeService.addEventListener(listener);
    }

}
