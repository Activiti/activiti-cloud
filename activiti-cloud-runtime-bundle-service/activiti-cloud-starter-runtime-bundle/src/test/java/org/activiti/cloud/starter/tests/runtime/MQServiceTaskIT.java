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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MQServiceTaskIT extends AbstractMQServiceTaskIT {

    @Test
    public void shouldConfigureDefaultConnectorBindingProperties() {
        //given

        //when
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();

        //then
        assertThat(bindings)
            .extractingFromEntries(entry ->
                new AbstractMap.SimpleEntry<String, String>(
                    entry.getKey(),
                    entry.getValue().getDestination()
                )
            )
            .contains(
                entry("mealsConnector", "mealsConnector"),
                entry("rest.GET", "rest.GET"),
                entry("perfromBusinessTask", "perfromBusinessTask"),
                entry("anyImplWithoutHandler", "anyImplWithoutHandler"),
                entry("payment", "payment"),
                entry(
                    "Constants Connector.constantsActionName",
                    "Constants Connector.constantsActionName"
                ),
                entry(
                    "Variable Mapping Connector.variableMappingActionName",
                    "Variable Mapping Connector.variableMappingActionName"
                ),
                entry("miCloudConnector", "miCloudConnector")
            );
    }
}
