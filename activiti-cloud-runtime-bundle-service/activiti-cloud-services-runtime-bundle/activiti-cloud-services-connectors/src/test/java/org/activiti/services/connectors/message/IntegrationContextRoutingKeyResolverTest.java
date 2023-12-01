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
package org.activiti.services.connectors.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.activiti.cloud.services.events.message.RuntimeBundleInfoMessageHeaders;
import org.junit.jupiter.api.Test;

public class IntegrationContextRoutingKeyResolverTest {

    private IntegrationContextRoutingKeyResolver subject = new IntegrationContextRoutingKeyResolver();

    @Test
    public void testResolveRoutingKeyFromValidHeadersInAnyOrder() {
        // given
        Map<String, Object> headers = MapBuilder
            .<String, Object>map(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "service-name")
            .with(IntegrationContextMessageHeaders.PROCESS_INSTANCE_ID, "process-instance-id")
            .with(RuntimeBundleInfoMessageHeaders.APP_NAME, "app-name")
            .with(IntegrationContextMessageHeaders.CONNECTOR_TYPE, "connector-type")
            .with(IntegrationContextMessageHeaders.BUSINESS_KEY, "business-key");
        // when
        String routingKey = subject.resolve(headers);

        // then
        assertThat(routingKey)
            .isEqualTo("integrationContext.service-name.app-name.connector-type.process-instance-id.business-key");
    }

    private static class MapBuilder<K, V> extends java.util.HashMap<K, V> {

        private static final long serialVersionUID = 1L;

        public MapBuilder<K, V> with(K key, V value) {
            put(key, value);
            return this;
        }

        public static <K, V> MapBuilder<K, V> map(K key, V value) {
            return new MapBuilder<K, V>().with(key, value);
        }

        public static <K, V> MapBuilder<K, V> emptyMap() {
            return new MapBuilder<K, V>();
        }
    }
}
