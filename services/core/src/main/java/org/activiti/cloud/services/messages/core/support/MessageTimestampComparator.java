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

package org.activiti.cloud.services.messages.core.support;

import java.util.Comparator;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class MessageTimestampComparator implements Comparator<Message<?>> {
    
    @Override
    public int compare(Message<?> o1, Message<?> o2) {
        Long sequenceNumber1 = getTimestamp(o1);
        Long sequenceNumber2 = getTimestamp(o2);

        return Long.compare(sequenceNumber1, sequenceNumber2);
    }
    
    @Nullable
    public Long getTimestamp(Message<?> m) {
        Object value = m.getHeaders().get(MessageHeaders.TIMESTAMP);
        if (value == null) {
            return null;
        }
        return (value instanceof Long ? (Long) value : Long.parseLong(value.toString()));
    }
    

}
