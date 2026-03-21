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
package org.activiti.cloud.services.query.rest.config;

import java.lang.reflect.Type;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * see: https://discourse.hibernate.org/t/missing-formatmapper-for-json-format-with-jackson-3-x-hibernate-7-x/11819/3
 */
public class Jackson3JsonFormatMapper extends AbstractJsonFormatMapper {

    private final JsonMapper jsonMapper;

    public Jackson3JsonFormatMapper() {
        this(JsonMapper.builder().build());
    }

    public Jackson3JsonFormatMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <T> T fromString(CharSequence charSequence, Type type) {
        try {
            return jsonMapper.readValue(charSequence.toString(), jsonMapper.constructType(type));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Could not deserialize string to java type: " + type, e);
        }
    }

    @Override
    public <T> String toString(T value, Type type) {
        try {
            return jsonMapper.writerFor(jsonMapper.constructType(type)).writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Could not serialize object of java type: " + type, e);
        }
    }
}
