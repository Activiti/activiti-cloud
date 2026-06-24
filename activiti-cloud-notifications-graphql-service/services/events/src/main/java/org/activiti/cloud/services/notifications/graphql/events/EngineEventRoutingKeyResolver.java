package org.activiti.cloud.services.notifications.graphql.events;

import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;

public class EngineEventRoutingKeyResolver implements RoutingKeyResolver {

    private static final String DEFAULT_VALUE = "_";

    @Override
    public String resolveRoutingKey(Object object) {
        if (object instanceof EngineEvent engineEvent) {
            return "engineEvents.%s.%s.%s.%s.%s.%s.%s".formatted(
                    engineEvent.getOrDefault("serviceName", DEFAULT_VALUE),
                    engineEvent.getOrDefault("appName", DEFAULT_VALUE),
                    engineEvent.getOrDefault("eventType", DEFAULT_VALUE),
                    engineEvent.getOrDefault("processDefinitionKey", DEFAULT_VALUE),
                    engineEvent.getOrDefault("processInstanceId", DEFAULT_VALUE),
                    engineEvent.getOrDefault("businessKey", DEFAULT_VALUE),
                    engineEvent.getOrDefault("actor", DEFAULT_VALUE)
                );
        }

        return null;
    }
}
