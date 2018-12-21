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
package org.activiti.cloud.services.events.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;


public class AuditProducerRoutingKeyResolverTest {

    private AuditProducerRoutingKeyResolver subject = new AuditProducerRoutingKeyResolver();
    
    @Test
    public void testResolveRoutingKeyFromValidHeadersInAnyOrder() {
        // given
        Map<String, Object> headers = MapBuilder.<String, Object> map(RuntimeBundleInfoMessageHeaders.APP_NAME, "app-name")
                                                .with(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "service-name")
                                                .with(ExecutionContextMessageHeaders.BUSINESS_KEY, "business-key")
                                                .with(ExecutionContextMessageHeaders.PROCESS_INSTANCE_ID, "process-instance-id")
                                                .with(ExecutionContextMessageHeaders.PROCESS_DEFINITION_KEY, "process-definition-key");
        
        // when
        String routingKey = subject.resolve(headers);
        
        // then
        assertThat(routingKey).isEqualTo("service-name.app-name.process-definition-key.process-instance-id.business-key");
                
    }

    @Test
    public void testResolveRoutingKeyFromEmptyHeaders() {
        // given
        Map<String, Object> headers = MapBuilder.<String, Object> map(RuntimeBundleInfoMessageHeaders.APP_NAME, "app-name")
                                                .with(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "service-name")
                                                .with(ExecutionContextMessageHeaders.PROCESS_DEFINITION_KEY, "process-definition-key")
                                                .with(ExecutionContextMessageHeaders.PROCESS_INSTANCE_ID, "process-instance-id")
                                                .with(ExecutionContextMessageHeaders.BUSINESS_KEY, "");
        
        // when
        String routingKey = subject.resolve(headers);
        
        // then
        assertThat(routingKey).isEqualTo("service-name.app-name.process-definition-key.process-instance-id._");
                
    }

    @Test
    public void testResolveRoutingKeyFromNullHeaders() {
        // given
        Map<String, Object> headers = MapBuilder.<String, Object> map(RuntimeBundleInfoMessageHeaders.APP_NAME, "app-name")
                                                .with(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "service-name")
                                                .with(ExecutionContextMessageHeaders.PROCESS_DEFINITION_KEY, "process-definition-key")
                                                .with(ExecutionContextMessageHeaders.PROCESS_INSTANCE_ID, "process-instance-id")
                                                .with(ExecutionContextMessageHeaders.BUSINESS_KEY, null);
        
        // when
        String routingKey = subject.resolve(headers);
        
        // then
        assertThat(routingKey).isEqualTo("service-name.app-name.process-definition-key.process-instance-id._");
                
    }

    @Test
    public void testResolveRoutingKeyWithEscapedValues() {
        // given
        Map<String, Object> headers = MapBuilder.<String, Object> map(RuntimeBundleInfoMessageHeaders.APP_NAME, "app:name")
                                                .with(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, "service.name")
                                                .with(ExecutionContextMessageHeaders.PROCESS_DEFINITION_KEY, "process#definition-key")
                                                .with(ExecutionContextMessageHeaders.PROCESS_INSTANCE_ID, "process*instance*id")
                                                .with(ExecutionContextMessageHeaders.BUSINESS_KEY, "business key");
        
        // when
        String routingKey = subject.resolve(headers);
        
        // then
        assertThat(routingKey).isEqualTo("service-name.app-name.process-definition-key.process-instance-id.business-key");
                
    }
    
    @Test
    public void testResolveRoutingKeyWithNonExistingHeaders() {
        // given
        Map<String, Object> headers = MapBuilder.<String, Object> emptyMap();
        
        // when
        String routingKey = subject.resolve(headers);
        
        // then
        assertThat(routingKey).isEqualTo("_._._._._");
                
    }
    
}
