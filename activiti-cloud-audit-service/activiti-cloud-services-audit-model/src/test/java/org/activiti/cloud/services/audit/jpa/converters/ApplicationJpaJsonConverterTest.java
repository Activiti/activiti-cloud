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
package org.activiti.cloud.services.audit.jpa.converters;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.test.Assertions.assertThat;

import org.activiti.api.process.model.Deployment;
import org.activiti.api.runtime.model.impl.DeploymentImpl;
import org.activiti.cloud.services.audit.jpa.converters.json.ApplicationJpaJsonConverter;
import org.junit.jupiter.api.Test;

public class ApplicationJpaJsonConverterTest {

    private ApplicationJpaJsonConverter converter = new ApplicationJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        DeploymentImpl deployment = new DeploymentImpl();
        deployment.setName("DeploymentName");
        deployment.setVersion(1);
        deployment.setId("DeploymentId");

        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(deployment);

        //then
        assertThatJson(jsonRepresentation)
            .node("name")
            .isEqualTo("DeploymentName")
            .node("version")
            .isEqualTo(1)
            .node("id")
            .isEqualTo("DeploymentId");
    }

    @Test
    public void convertToEntityAttributeShouldCreateAnApplicationWithFieldsSet() throws Exception {
        //given
        String jsonRepresentation = "{\"id\":\"DeploymentId\"," + "\"version\":\"1\"," + "\"name\":\"DeploymentName\"}";

        //when
        Deployment deployment = converter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(deployment).isNotNull().hasId("DeploymentId").hasName("DeploymentName").hasVersion(1);
    }
}
