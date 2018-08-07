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

package org.activiti.cloud.organization.config;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.NameTransformer;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.impl.ApplicationImpl;

/**
 * Deserializer for {@link Application} resources
 */
public class ApplicationDeserializer extends JsonDeserializer<Application> {

    @Override
    public Application deserialize(JsonParser parser,
                                   DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        ObjectNode root = mapper.readTree(parser);
        return mapper.readValue(root.toString(),
                                ApplicationImpl.class);
    }

    @Override
    public JsonDeserializer<Application> unwrappingDeserializer(NameTransformer unwrapper) {
        // We need to create a unwrappingDeserializer for supporting
        // HAL Resource deserialization which unwrap the content.
        return new ApplicationDeserializer();
    }
}
