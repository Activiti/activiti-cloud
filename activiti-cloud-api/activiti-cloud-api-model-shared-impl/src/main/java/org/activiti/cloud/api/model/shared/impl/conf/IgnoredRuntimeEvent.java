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
package org.activiti.cloud.api.model.shared.impl.conf;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;

public class IgnoredRuntimeEvent
    extends CloudRuntimeEventImpl<Void, IgnoredRuntimeEvent.IgnoredRuntimeEvents>
    implements CloudRuntimeEvent<Void, IgnoredRuntimeEvent.IgnoredRuntimeEvents> {

    public enum IgnoredRuntimeEvents {
        IGNORED,
    }

    //set by jackson via reflection
    private String eventType;

    @Override
    public IgnoredRuntimeEvents getEventType() {
        return IgnoredRuntimeEvents.IGNORED;
    }

    public String getIgnoredEventTypeAsString() {
        return eventType;
    }
}
