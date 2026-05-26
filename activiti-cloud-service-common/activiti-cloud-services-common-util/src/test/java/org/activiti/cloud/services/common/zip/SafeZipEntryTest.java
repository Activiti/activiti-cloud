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

import org.junit.jupiter.api.Test;

class SafeZipEntryTest {

    @Test
    void toString_shouldIncludeContentBytes() {
        SafeZipEntry entry = new SafeZipEntry("file.json", "{\"a\":1}".getBytes(UTF_8));

        assertThat(entry.toString()).contains("file.json").contains("123, 34, 97, 34, 58, 49, 125");
    }

    @Test
    void toString_shouldRepresentDirectoryWithoutContent() {
        SafeZipEntry entry = new SafeZipEntry("folder/", null, true);

        assertThat(entry.toString()).contains("folder/").contains("directory=true").contains("null");
    }

    @Test
    void equals_shouldCompareContentByValue() {
        SafeZipEntry first = new SafeZipEntry("a.json", "x".getBytes(UTF_8));
        SafeZipEntry second = new SafeZipEntry("a.json", "x".getBytes(UTF_8));

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
