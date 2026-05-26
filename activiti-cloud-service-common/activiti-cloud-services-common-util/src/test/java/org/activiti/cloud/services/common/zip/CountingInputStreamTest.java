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
package org.activiti.cloud.services.common.zip;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class CountingInputStreamTest {

    @Test
    void read_shouldIncrementCount_forSingleByteReads() throws IOException {
        CountingInputStream stream = new CountingInputStream(new ByteArrayInputStream("ab".getBytes(UTF_8)));

        assertThat(stream.read()).isEqualTo('a');
        assertThat(stream.read()).isEqualTo('b');
        assertThat(stream.read()).isEqualTo(-1);
        assertThat(stream.getCount()).isEqualTo(2);
    }

    @Test
    void read_shouldNotIncrementCount_whenEndOfStream() throws IOException {
        CountingInputStream stream = new CountingInputStream(new ByteArrayInputStream(new byte[0]));

        assertThat(stream.read()).isEqualTo(-1);
        assertThat(stream.getCount()).isZero();
    }

    @Test
    void read_shouldIncrementCount_forArrayReads() throws IOException {
        byte[] source = "hello".getBytes(UTF_8);
        CountingInputStream stream = new CountingInputStream(new ByteArrayInputStream(source));
        byte[] buffer = new byte[3];

        assertThat(stream.read(buffer, 0, buffer.length)).isEqualTo(3);
        assertThat(stream.read(buffer, 0, buffer.length)).isEqualTo(2);
        assertThat(stream.read(buffer, 0, buffer.length)).isEqualTo(-1);
        assertThat(stream.getCount()).isEqualTo(source.length);
    }

    @Test
    void read_shouldNotIncrementCount_whenNoBytesRead() throws IOException {
        CountingInputStream stream = new CountingInputStream(new ByteArrayInputStream("x".getBytes(UTF_8)));
        byte[] buffer = new byte[4];

        assertThat(stream.read(buffer, 0, 0)).isZero();
        assertThat(stream.getCount()).isZero();
    }
}
