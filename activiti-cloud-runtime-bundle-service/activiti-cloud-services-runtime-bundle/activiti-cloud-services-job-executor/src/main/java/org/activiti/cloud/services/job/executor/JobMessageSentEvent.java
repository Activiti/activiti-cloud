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
package org.activiti.cloud.services.job.executor;

import java.util.Objects;
import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;

public class JobMessageSentEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    private final Message<?> message;

    public JobMessageSentEvent(Message<?> message, Object source) {
        super(source);
        this.message = message;
    }

    public Message<?> getMessage() {
        return message;
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        JobMessageSentEvent other = (JobMessageSentEvent) obj;
        return Objects.equals(message, other.message);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("JobMessageSentEvent [message=");
        builder.append(message);
        builder.append(", source=");
        builder.append(source);
        builder.append("]");
        return builder.toString();
    }
}
