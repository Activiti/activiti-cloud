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

class SafeZipStreamEntryTest {

    @Test
    void shouldExposeFileAndFolderMetadata_forNestedFileEntry() {
        SafeZipStreamEntry entry = new SafeZipStreamEntry("folder/file.txt", "data".getBytes(UTF_8), false);

        assertThat(entry.getName()).isEqualTo("folder/file.txt");
        assertThat(entry.getFileName()).isEqualTo("file.txt");
        assertThat(entry.getFolderName(0)).contains("folder");
        assertThat(entry.getContent()).contains("data".getBytes(UTF_8));
        assertThat(entry.isDirectory()).isFalse();
        assertThat(entry.isAtRoot()).isFalse();
    }

    @Test
    void shouldExposeDirectoryMetadata_withoutContent() {
        SafeZipStreamEntry entry = new SafeZipStreamEntry("folder/", null, true);

        assertThat(entry.getFileName()).isEqualTo("folder");
        assertThat(entry.getContent()).isEmpty();
        assertThat(entry.isDirectory()).isTrue();
    }
}
