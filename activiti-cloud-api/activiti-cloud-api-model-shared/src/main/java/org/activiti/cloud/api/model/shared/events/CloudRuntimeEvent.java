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
package org.activiti.cloud.api.model.shared.events;

import org.activiti.api.model.shared.event.RuntimeEvent;
import org.activiti.cloud.api.model.shared.CloudRuntimeEntity;

public interface CloudRuntimeEvent<ENTITY_TYPE, EVENT_TYPE extends Enum<?>>
    extends CloudRuntimeEntity, RuntimeEvent<ENTITY_TYPE, EVENT_TYPE> {
    /**
     * Sequence index of the event if it is part of an aggregate within the message if part of the same transaction.
     */
    Integer getSequenceNumber();

    /**
     * ID of the message that carried the event, all the events that are part of the same tx are aggregated in the same message.
     */
    String getMessageId();

    /**
     * ID of the entity included in the message.
     */
    String getEntityId();
}
