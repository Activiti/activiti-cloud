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

package org.activiti.cloud.starters.test.builder;

import org.activiti.cloud.starters.test.MockActivityEvent;

public class ActivityEventBuilder extends MockProcessEngineEventBuilder<MockActivityEvent, ActivityEventBuilder> {

    private ActivityEventBuilder(Long timestamp, String eventType) {
        super(timestamp, eventType);
    }

    public static ActivityEventBuilder aActivityStartedEvent(Long timestamp) {
        return new ActivityEventBuilder(timestamp, "ActivityStartedEvent");
    }

    @Override
    protected MockActivityEvent createInstance(Long timestamp,
                                               String eventType) {
        return new MockActivityEvent(timestamp, eventType);
    }

    public ActivityEventBuilder withName(String name) {
        getEvent().setActivityName(name);
        return this;
    }

}
