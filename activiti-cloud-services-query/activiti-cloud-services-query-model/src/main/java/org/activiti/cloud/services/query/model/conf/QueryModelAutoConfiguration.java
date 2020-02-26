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

package org.activiti.cloud.services.query.model.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.query.model.VariableValueJsonConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryModelAutoConfiguration {

    @Bean
    public VariableValueJsonConverter variableValueJsonConverter(ObjectMapper objectMapper) {
        //this bean is not directly used as it's instantiated by Hibernate as a converter.
        //it's only here as a workaround to be able to inject the object mapper
        return new VariableValueJsonConverter(objectMapper);
    }

}
