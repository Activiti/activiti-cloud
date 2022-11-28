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

package org.activiti.cloud.services.messages.core.integration;

import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.messaging.Message;

public class EmptyErrorChannelHeaderFilter extends HeaderFilter {

    private static String ERROR_CHANNEL = "errorChannel";

    public EmptyErrorChannelHeaderFilter() {
        super(ERROR_CHANNEL);
    }

    @Override
    public Message<?> transform(Message<?> message) {
        if ("".equals(message.getHeaders().get(ERROR_CHANNEL))) {
            return super.transform(message);
        } else {
            return message;
        }
    }

}
