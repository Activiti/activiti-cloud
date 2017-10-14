package org.activiti.starters.test;

import org.activiti.engine.impl.event.logger.handler.VariableCreatedEventHandler;

public class VariableCreatedEventBuilder extends VariableEventBuilder<MockVariableCreatedEvent, VariableCreatedEventHandler>{

    private MockVariableCreatedEvent event;

    private VariableCreatedEventBuilder(long timestamp) {
        event = new MockVariableCreatedEvent(timestamp);
    }

    public static VariableCreatedEventBuilder aVariableCreatedEvent(long timestamp) {
        return new VariableCreatedEventBuilder(timestamp);
    }

    @Override
    protected MockVariableCreatedEvent getEvent() {
        return event;
    }

}
