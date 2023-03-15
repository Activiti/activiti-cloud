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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class AuditProducerRoutingKeyResolverTest {

    private AuditProducerRoutingKeyResolver subject = new AuditProducerRoutingKeyResolver();

    @Test
    public void testResolveRoutingKeyFromValidHeadersInAnyOrder() {
        // given
        Map<String, Object> headers = MapBuilder
            .<String, Object>map(RuntimeBundleInfoMessageHeaders.APP_NAME, "app-name")
            .with(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "service-name");

        // when
        String routingKey = subject.resolve(headers);

        // then
        assertThat(routingKey).isEqualTo("engineEvents.service-name.app-name");
    }

    @Test
    public void testResolveRoutingKeyFromEmptyHeaders() {
        // given
        Map<String, Object> headers = MapBuilder
            .<String, Object>map(RuntimeBundleInfoMessageHeaders.APP_NAME, "")
            .with(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "service-name");

        // when
        String routingKey = subject.resolve(headers);

        // then
        assertThat(routingKey).isEqualTo("engineEvents.service-name._");
    }

    @Test
    public void testResolveRoutingKeyFromNullHeaders() {
        // given
        Map<String, Object> headers = MapBuilder
            .<String, Object>map(RuntimeBundleInfoMessageHeaders.APP_NAME, null)
            .with(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "service-name");

        // when
        String routingKey = subject.resolve(headers);

        // then
        assertThat(routingKey).isEqualTo("engineEvents.service-name._");
    }

    @Test
    public void testResolveRoutingKeyWithEscapedValues() {
        // given
        Map<String, Object> headers = MapBuilder
            .<String, Object>map(RuntimeBundleInfoMessageHeaders.APP_NAME, "app:na#me")
            .with(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "ser.vice*na me");

        // when
        String routingKey = subject.resolve(headers);

        // then
        assertThat(routingKey).isEqualTo("engineEvents.ser-vice-na-me.app-na-me");
    }

    @Test
    public void testResolveRoutingKeyWithNonExistingHeaders() {
        // given
        Map<String, Object> headers = MapBuilder.<String, Object>emptyMap();

        // when
        String routingKey = subject.resolve(headers);

        // then
        assertThat(routingKey).isEqualTo("engineEvents._._");
    }
}
