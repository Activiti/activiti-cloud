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
package org.activiti.cloud.services.modeling.validation;

import static org.activiti.cloud.modeling.api.ValidationContext.EMPTY_CONTEXT;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;

import java.io.IOException;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.config.ModelingApiAutoConfiguration;
import org.everit.json.schema.Schema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = { JsonSchemaModelValidatorConfiguration.class, ModelingApiAutoConfiguration.class })
public class ConnectorModelValidatorIT {

    @Autowired
    @Qualifier("connectorSchema")
    public Schema connectorSchema;

    @Autowired
    public ConnectorModelType connectorModelType;

    @Test
    public void should_notThrowException_when_validatingAValidConnectorWithExtendedProperties() throws IOException {
        ConnectorModelValidator connectorModelValidator = new ConnectorModelValidator(
            connectorSchema,
            connectorModelType
        );

        connectorModelValidator.validateModelContent(
            resourceAsByteArray("connector/connector-with-model.json"),
            EMPTY_CONTEXT
        );
    }
}
