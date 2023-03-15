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
package org.activiti.cloud.api.model.shared.impl.events;

import org.activiti.api.model.shared.event.VariableEvent;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;

public class CloudVariableUpdatedEventImpl<T> extends CloudVariableEventImpl implements CloudVariableUpdatedEvent {

    private T previousValue;

    public CloudVariableUpdatedEventImpl() {}

    public CloudVariableUpdatedEventImpl(VariableInstance entity, T previousValue) {
        super(entity);
        this.previousValue = previousValue;
    }

    public CloudVariableUpdatedEventImpl(String id, Long timestamp, VariableInstance entity, T previousValue) {
        super(id, timestamp, entity);
        this.previousValue = previousValue;
    }

    @Override
    public VariableEvent.VariableEvents getEventType() {
        return VariableEvent.VariableEvents.VARIABLE_UPDATED;
    }

    @Override
    public T getPreviousValue() {
        return previousValue;
    }
}
