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
package org.activiti.cloud.services.events.message;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

public class RuntimeBundleInfoMessageBuilderAppender implements MessageBuilderAppender {

    private final RuntimeBundleProperties properties;

    public RuntimeBundleInfoMessageBuilderAppender(RuntimeBundleProperties properties) {
        Assert.notNull(properties, "properties must not be null");

        this.properties = properties;
    }

    @Override
    public <P> MessageBuilder<P> apply(MessageBuilder<P> request) {
        Assert.notNull(request, "request must not be null");

        return request
            .setHeader(RuntimeBundleInfoMessageHeaders.APP_NAME, properties.getAppName())
            .setHeader(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, properties.getServiceName())
            .setHeader(RuntimeBundleInfoMessageHeaders.SERVICE_FULL_NAME, properties.getServiceFullName())
            .setHeader(RuntimeBundleInfoMessageHeaders.SERVICE_TYPE, properties.getServiceType())
            .setHeader(RuntimeBundleInfoMessageHeaders.SERVICE_VERSION, properties.getServiceVersion());
    }
}
