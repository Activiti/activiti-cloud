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
package org.activiti.cloud.common.messaging.config.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.activiti.cloud.common.messaging.config.OutputBindingConfiguration;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@SpringBootTest(
    properties = {
        "activiti.cloud.application.name=test-app",
        "activiti.cloud.messaging.connectors.rest-conn.binding-key=testProducer_source-out-0",
        "activiti.cloud.messaging.connectors.rest-conn.destination=exchange_name",
        "activiti.cloud.messaging.connectors.rest-conn.required-groups=queue_group_name",
    }
)
@Import(
    {
        TestChannelBinderConfiguration.class,
        OutputBindingDuplicatePreventionIT.TestConfig.class,
        OutputBindingConfiguration.class,
    }
)
class OutputBindingDuplicatePreventionIT {

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    void should_notAddDuplicate_when_bindingAlreadyInOutputBindings() {
        String outputBindings = bindingServiceProperties.getOutputBindings();
        assertThat(outputBindings).isNotNull();

        long occurrences = Arrays.stream(outputBindings.split(";")).filter("testProducer_source-out-0"::equals).count();

        assertThat(occurrences).as("testProducer_source should appear exactly once (not duplicated)").isEqualTo(1);

        String[] bindings = outputBindings.split(";");
        long uniqueCount = Arrays.stream(bindings).distinct().count();
        assertThat(bindings).as("output-bindings should not contain any duplicates").hasSize((int) uniqueCount);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @OutputBinding("testProducer")
        public MessageChannel testProducer() {
            return new DirectChannel();
        }
    }
}
