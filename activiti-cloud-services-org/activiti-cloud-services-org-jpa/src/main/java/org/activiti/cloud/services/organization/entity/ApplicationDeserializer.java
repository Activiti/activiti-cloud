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

package org.activiti.cloud.services.organization.entity;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.activiti.cloud.organization.repository.entity.Application;
import org.springframework.stereotype.Component;

/**
 * Deserializer for {@link Application} resources
 */
@Component
public class ApplicationDeserializer extends JsonDeserializer<Application> {

    @Override
    public Application deserialize(JsonParser parser,
                                   DeserializationContext context) throws IOException {
        return parser.getCodec().readValue(parser,
                                           ApplicationEntity.class);
    }
}
