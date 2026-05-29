/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties.FunctionRouterProperties;
import org.junit.jupiter.api.Test;

class FunctionRouterPropertiesTest {

    @Test
    void should_be_equal_to_itself() {
        FunctionRouterProperties a = new FunctionRouterProperties();

        assertThat(a.equals(a)).isTrue();
    }

    @Test
    void should_not_be_equal_to_null() {
        FunctionRouterProperties a = new FunctionRouterProperties();

        assertThat(a.equals(null)).isFalse();
    }

    @Test
    void should_not_be_equal_to_different_type() {
        FunctionRouterProperties a = new FunctionRouterProperties();

        assertThat(a.equals("not-a-FunctionRouterProperties")).isFalse();
    }

    @Test
    void should_not_be_equal_when_enabled_differs() {
        FunctionRouterProperties enabled = new FunctionRouterProperties();
        FunctionRouterProperties disabled = new FunctionRouterProperties();

        enabled.setEnabled(true);
        disabled.setEnabled(false);

        assertThat(enabled.equals(disabled)).isFalse();
    }

    @Test
    void should_not_be_equal_when_maxRetries_differs() {
        FunctionRouterProperties a = new FunctionRouterProperties();
        FunctionRouterProperties b = new FunctionRouterProperties();

        a.setMaxRetries(5);
        b.setMaxRetries(10);

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void should_not_be_equal_when_group_differs() {
        FunctionRouterProperties a = new FunctionRouterProperties();
        FunctionRouterProperties b = new FunctionRouterProperties();

        a.setGroup("group-a");
        b.setGroup("group-b");

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void should_not_be_equal_when_retryInterval_differs() {
        FunctionRouterProperties a = new FunctionRouterProperties();
        FunctionRouterProperties b = new FunctionRouterProperties();

        a.setRetryInterval(Duration.ofMillis(100));
        b.setRetryInterval(Duration.ofMillis(200));

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void should_not_be_equal_when_processingTimeout_differs() {
        FunctionRouterProperties a = new FunctionRouterProperties();
        FunctionRouterProperties b = new FunctionRouterProperties();

        a.setProcessingTimeout(Duration.ofSeconds(30));
        b.setProcessingTimeout(Duration.ofSeconds(60));

        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void should_not_be_equal_when_routes_differ() {
        FunctionRouterProperties a = new FunctionRouterProperties();
        FunctionRouterProperties b = new FunctionRouterProperties();

        a.getRoutes().put("route-a", new ActivitiCloudMessagingProperties.BindingFunctionRouterProperties());

        assertThat(a.equals(b)).isFalse();
    }
}
