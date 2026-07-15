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
package org.activiti.cloud.services.query.model;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapOfStringObjectJsonConverterTest {

    @Test
    void convertToDatabaseColumnShouldConvertJava8DateTime() {
        MapOfStringObjectJsonConverter converter = new MapOfStringObjectJsonConverter();
        LocalDateTime localDateTime = LocalDateTime.of(2000, 1, 1, 1, 1);

        String date = converter.convertToDatabaseColumn(Map.of("date", localDateTime));

        assertThat(date).startsWith("{\"date\":\"2000-01-01T01:01:00\"}");
    }

    @Test
    void convertToEntityAttributeShouldRejectDataExceedingMaxSize() {
        MapOfStringObjectJsonConverter converter = new MapOfStringObjectJsonConverter();
        String oversizedData = "{\"key\":\"" + "x".repeat(MapOfStringObjectJsonConverter.MAX_DESERIALIZE_SIZE) + "\"}";

        assertThatThrownBy(() -> converter.convertToEntityAttribute(oversizedData))
            .isInstanceOf(QueryException.class)
            .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    void convertToEntityAttributeShouldDeserializeValidData() {
        MapOfStringObjectJsonConverter converter = new MapOfStringObjectJsonConverter();

        Map<String, Object> result = converter.convertToEntityAttribute("{\"key\":\"value\"}");

        assertThat(result).isEqualTo(Map.of("key", "value"));
    }
}
