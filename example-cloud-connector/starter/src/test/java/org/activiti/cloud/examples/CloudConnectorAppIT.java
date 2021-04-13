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
package org.activiti.cloud.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.examples.connectors.CustomPojo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CloudConnectorAppIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String appName;

    @Test
    public void contextShouldLoad() throws Exception {
        //then
        assertThat(context).isNotNull();
        assertThat(appName).isNotEmpty();
    }

    @Test
    public void shouldConvertExpectedJsonToPojo() throws IOException {
        String json = "{ \"test-json-variable-element1\":\"test-json-variable-value1\"}";
        Object jsonValue = objectMapper.readValue(json, Object.class);
        CustomPojo customPojo = objectMapper.convertValue(jsonValue, CustomPojo.class);
        assertThat(customPojo).isNotNull();
    }

}
