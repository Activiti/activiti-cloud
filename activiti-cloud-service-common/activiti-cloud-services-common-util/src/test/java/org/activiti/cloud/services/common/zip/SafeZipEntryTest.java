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
    void equals_shouldCompareContentByValue() {
        SafeZipEntry first = new SafeZipEntry("a.json", "x".getBytes(UTF_8));
        SafeZipEntry second = new SafeZipEntry("a.json", "x".getBytes(UTF_8));

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void equals_shouldReturnTrueForSameInstance() {
        SafeZipEntry entry = new SafeZipEntry("a.json", "x".getBytes(UTF_8));

        assertThat(entry.equals(entry)).isTrue();
    }

    @Test
    void equals_shouldReturnFalseForDifferentType() {
        SafeZipEntry entry = new SafeZipEntry("a.json", "x".getBytes(UTF_8));

        assertThat(entry.equals("a.json")).isFalse();
    }

    @Test
    void equals_shouldReturnFalseWhenNameDiffers() {
        SafeZipEntry first = new SafeZipEntry("a.json", "x".getBytes(UTF_8));
        SafeZipEntry second = new SafeZipEntry("b.json", "x".getBytes(UTF_8));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_shouldReturnFalseWhenContentDiffers() {
        SafeZipEntry first = new SafeZipEntry("a.json", "x".getBytes(UTF_8));
        SafeZipEntry second = new SafeZipEntry("a.json", "y".getBytes(UTF_8));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_shouldReturnFalseWhenDirectoryFlagDiffers() {
        SafeZipEntry file = new SafeZipEntry("folder/", null, false);
        SafeZipEntry directory = new SafeZipEntry("folder/", null, true);

        assertThat(file).isNotEqualTo(directory);
    }

    @Test
    void twoArgConstructor_shouldDefaultDirectoryToFalse() {
        SafeZipEntry entry = new SafeZipEntry("a.json", "x".getBytes(UTF_8));

        assertThat(entry.directory()).isFalse();
    }

    @Test
    void content_shouldReturnNullForDirectoryEntry() {
        SafeZipEntry directory = new SafeZipEntry("folder/", null, true);

        assertThat(directory.content()).isNull();
    }

    @Test
    void content_shouldReturnDefensiveCopy() {
        byte[] original = "payload".getBytes(UTF_8);
        SafeZipEntry entry = new SafeZipEntry("a.json", original);

        byte[] returned = entry.content();
        original[0] = 'X';
        returned[0] = 'Y';

        assertThat(entry.content()).isEqualTo("payload".getBytes(UTF_8));
    }

    @Test
    void constructor_shouldCopyContentOnCreation() {
        byte[] original = "payload".getBytes(UTF_8);
        SafeZipEntry entry = new SafeZipEntry("a.json", original);
        original[0] = 'X';

        assertThat(entry.content()).isEqualTo("payload".getBytes(UTF_8));
    }

    @Test
    void hashCode_shouldBeConsistentForDirectoryEntry() {
        SafeZipEntry first = new SafeZipEntry("folder/", null, true);
        SafeZipEntry second = new SafeZipEntry("folder/", null, true);

        assertThat(first).hasSameHashCodeAs(second);
    }
}
