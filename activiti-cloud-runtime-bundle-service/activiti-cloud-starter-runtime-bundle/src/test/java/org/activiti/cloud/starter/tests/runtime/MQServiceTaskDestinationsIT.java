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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.AbstractMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.config.BindingProperties;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "ACT_MESSAGING_DEST_TRANSFORMERS_ENABLED=true",
        "ACT_MESSAGING_DEST_TRANSFORMERS=toLowerCase,escapeIllegalChars",
        "ACT_MESSAGING_DEST_SEPARATOR=.",
        "ACT_MESSAGING_DEST_PREFIX=MQServiceTaskDestinationsIT",
        // Connector implementation producer binding. Use [] brackets combined with escaping the whitespace using either \  or \u0020
        "spring.cloud.stream.bindings.[Constants\\ Connector.constantsActionName].destination=Constants Connector.constantsActionName",
        // Connector implementation consumer binding
        "spring.cloud.stream.bindings.constantsIntegrationEventsConsumer.destination=Constants Connector.constantsActionName",
        // Configure connector messaging destination with custom value
        "activiti.cloud.messaging.destinations.[Constants\\ Connector.constantsActionName].name=constants_connector.constants_action_name",
    }
)
public class MQServiceTaskDestinationsIT extends AbstractMQServiceTaskIT {

    @Test
    public void shouldConfigureAndTransformConnectorBindingProperties() {
        //given

        //when
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();

        //then
        assertThat(bindings)
            .extractingFromEntries(entry ->
                new AbstractMap.SimpleEntry<String, String>(entry.getKey(), entry.getValue().getDestination())
            )
            .contains(
                entry("mealsConnector", "mqservicetaskdestinationsit.mealsconnector"),
                entry("rest.GET", "mqservicetaskdestinationsit.rest.get"),
                entry("perfromBusinessTask", "mqservicetaskdestinationsit.perfrombusinesstask"),
                entry("anyImplWithoutHandler", "mqservicetaskdestinationsit.anyimplwithouthandler"),
                entry("payment", "mqservicetaskdestinationsit.payment"),
                entry(
                    "Variable Mapping Connector.variableMappingActionName",
                    "mqservicetaskdestinationsit.variable-mapping-connector.variablemappingactionname"
                ),
                entry("miCloudConnector", "mqservicetaskdestinationsit.micloudconnector")
            );
    }

    @Test
    public void shouldConfigureCustomConnectorBindingProperties() {
        //given

        //when
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();

        //then
        assertThat(bindings)
            .extractingFromEntries(entry ->
                new AbstractMap.SimpleEntry<String, String>(entry.getKey(), entry.getValue().getDestination())
            )
            .contains(
                entry(
                    "Constants Connector.constantsActionName",
                    "mqservicetaskdestinationsit.constants_connector.constants_action_name"
                ),
                entry(
                    "constantsIntegrationEventsConsumer",
                    "mqservicetaskdestinationsit.constants_connector.constants_action_name"
                )
            );
    }
}
