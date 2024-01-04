package org.activiti.cloud.services.query.model;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapOfStringObjectJsonConverterTest {

    @Test
    void convertToDatabaseColumnShouldConvertJava8DateTime() {
        MapOfStringObjectJsonConverter converter = new MapOfStringObjectJsonConverter();
        LocalDateTime localDateTime = LocalDateTime.of(2000, 1, 1, 1, 1);

        String date = converter.convertToDatabaseColumn(Map.of("date", localDateTime));

        assertThat(date).startsWith("{\"date\":[2000,1,1,1,1]}");
    }
}
