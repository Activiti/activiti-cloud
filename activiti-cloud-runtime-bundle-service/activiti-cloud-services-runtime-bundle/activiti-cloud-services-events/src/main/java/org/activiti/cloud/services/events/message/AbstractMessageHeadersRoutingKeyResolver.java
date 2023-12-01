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
package org.activiti.cloud.services.events.message;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractMessageHeadersRoutingKeyResolver implements RoutingKeyResolver<Map<String, Object>> {

    private static final String REPLACEMENT = "-";
    private static final String ILLEGAL_CHARACTERS = "[\\t\\s\\.*#:]";
    private static final String DELIMITER = ".";
    private static final String UNDERSCORE = "_";

    public abstract String resolve(Map<String, Object> headers);

    protected String build(Map<String, Object> headers, String... keys) {
        return (
            getPrefix() +
            DELIMITER +
            Stream
                .of(keys)
                .map(headers::get)
                .map(Optional::ofNullable)
                .map(this::mapNullOrEmptyValue)
                .collect(Collectors.joining(DELIMITER))
        );
    }

    private String mapNullOrEmptyValue(Optional<Object> obj) {
        return obj
            .map(Object::toString)
            .filter(value -> !value.isEmpty())
            .map(this::escapeIllegalCharacters)
            .orElse(UNDERSCORE);
    }

    protected String escapeIllegalCharacters(String value) {
        return value.replaceAll(ILLEGAL_CHARACTERS, REPLACEMENT);
    }

    public abstract String getPrefix();
}
