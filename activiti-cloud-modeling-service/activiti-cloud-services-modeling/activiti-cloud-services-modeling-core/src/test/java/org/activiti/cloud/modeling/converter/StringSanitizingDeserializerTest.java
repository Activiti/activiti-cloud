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

package org.activiti.cloud.modeling.converter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StringSanitizingDeserializerTest {

    private static final String SVG =
        "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmci" +
        "IHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0Ij48ZyBpZD0ic2F2ZS0yNHB4XzFfIiBkYXRhLW5hbWU9InNh" +
        "dmUtMjRweCAoMSkiIG9wYWNpdHk9IjEiPjxwYXRoIGlkPSJQYXRoXzE3OTY1IiBkYXRhLW5hbWU9IlBhdGggMTc5NjUiIGQ9Ik0wLDBI" +
        "MjRWMjRIMFoiIGZpbGw9Im5vbmUiLz48cGF0aCBpZD0iUGF0aF8xNzk2NiIgZGF0YS1uYW1lPSJQYXRoIDE3OTY2IiBkPSJNMTcsM0g1" +
        "QTIsMiwwLDAsMCwzLDVWMTlhMiwyLDAsMCwwLDIsMkgxOWEyLjAwNiwyLjAwNiwwLDAsMCwyLTJWN1ptMiwxNkg1VjVIMTYuMTdMMTks" +
        "Ny44M1ptLTctN2EzLDMsMCwxLDAsMywzQTMsMywwLDAsMCwxMiwxMlpNNiw2aDl2NEg2WiIvPjwvZz48L3N2Zz4K";

    private static final String PNG =
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAAIGNIUk0A" +
        "AHomAACAhAAA+gAAAIDoAAB1MAAA6mAAADqYAAAXcJy6UTwAAAAEZ0FNQQAAsY58+1GTAAAAAXNSR0IArs4c6QAAAAZiS0dEAP8A/wD/" +
        "oL2nkwAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAMVJREFUeNpjYBgFBAAjGt8TiGcBsQyJ5jQAcSM2CWY0/j4gliXDoQ5Q+iC6BAsaXxaH" +
        "z/CBcCBeAvUFA7pPWPBo/E9kEK+EsrFawkSluARZEoMUHwzUtoAByScMtLIAK2AhIQmTBQbUB8iADZoy4qH8hUBcD8S/qGVBExCXI/Er" +
        "oEFYQa0giiVSjP5xQKwFi4kUIzsO6qBFB3okU80CUGqphGKKkuljaIn6n4Jgf4yvPrgBxPZAzEem4U+AOA2I74zW5UQDACHHHaj63Wy2" +
        "AAAAAElFTkSuQmCC";
    //change2
    private static final String SIMPLE_TEXT = "simple text";

    private static final String WRONG_SVG = "data:image/svg+xml;base64,SGVsbG8gdGhlcmUK";

    private StringSanitizingDeserializer stringSanitizingDeserializer = new StringSanitizingDeserializer();

    @Test
    public void should_sanitizeConvertSvgStringToPng() throws Exception {
        JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.hasToken(eq(JsonToken.VALUE_STRING))).thenReturn(true);
        when(jsonParser.getText()).thenReturn(SVG);
        String result = stringSanitizingDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));
        Assertions.assertThat(result).isEqualTo(PNG);
    }

    @Test
    public void should_omitSanitization_when_stringIsNotSvg() throws Exception {
        JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.hasToken(eq(JsonToken.VALUE_STRING))).thenReturn(true);
        when(jsonParser.getText()).thenReturn(SIMPLE_TEXT);
        String result = stringSanitizingDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));
        Assertions.assertThat(result).isEqualTo(SIMPLE_TEXT);
    }

    @Test
    public void should_omitSanitization_when_exceptionIsThrown() throws Exception {
        JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.hasToken(eq(JsonToken.VALUE_STRING))).thenReturn(true);
        when(jsonParser.getText()).thenReturn(WRONG_SVG);
        String result = stringSanitizingDeserializer.deserialize(jsonParser, mock(DeserializationContext.class));
        Assertions.assertThat(result).isEqualTo(WRONG_SVG);
    }
}
