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

package org.activiti.cloud.starter.tests.runtime;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

public interface CanFailConnectorChannels {
    String CAN_FAIL_CONNECTOR = "canFailConnector";

    @InputBinding(CAN_FAIL_CONNECTOR)
    default SubscribableChannel canFailConnector() {
        return MessageChannels.publishSubscribe(CAN_FAIL_CONNECTOR).getObject();
    }
}
