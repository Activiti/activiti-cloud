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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ZipEntryPathsTest {

    @Test
    void normalizeEntryName_shouldReplaceBackslashes() {
        assertThat(ZipEntryPaths.normalizeEntryName("folder\\file.txt")).isEqualTo("folder/file.txt");
    }

    @Test
    void isDirectoryEntry_shouldDetectTrailingSlash() {
        assertThat(ZipEntryPaths.isDirectoryEntry("folder/", false)).isTrue();
    }

    @Test
    void isDirectoryEntry_shouldDetectTrailingBackslash() {
        assertThat(ZipEntryPaths.isDirectoryEntry("folder\\", false)).isTrue();
    }

    @Test
    void isDirectoryEntry_shouldDetectZipDirectoryFlag() {
        assertThat(ZipEntryPaths.isDirectoryEntry("entry", true)).isTrue();
    }

    @Test
    void isDirectoryEntry_shouldReturnFalse_forRegularFile() {
        assertThat(ZipEntryPaths.isDirectoryEntry("file.txt", false)).isFalse();
    }

    @Test
    void getFileName_shouldReturnLeafName_forNestedPath() {
        assertThat(ZipEntryPaths.getFileName("folder/nested/file.txt", false)).isEqualTo("file.txt");
    }

    @Test
    void getFolderName_shouldReturnSegmentAtLevel() {
        assertThat(ZipEntryPaths.getFolderName("folder/nested/file.txt", false, 0)).contains("folder");
        assertThat(ZipEntryPaths.getFolderName("folder/nested/file.txt", false, 1)).contains("nested");
    }

    @Test
    void hasUnsafeFlatPath_shouldReturnFalse_forSimpleFileName() {
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("file.json")).isFalse();
    }

    @Test
    void hasUnsafeFlatPath_shouldReturnTrue_forNestedPaths() {
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("folder/file.json")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("folder\\file.json")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("..file.json")).isTrue();
    }

    @Test
    void hasUnsafeFlatPath_shouldRejectDriveLetterAndMalformedPaths() {
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("C:evil.json")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("bad\u0000name.json")).isTrue();
    }

    @Test
    void hasUnsafeHierarchicalPath_shouldDetectTraversalAndAbsolutePaths() {
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("../secret.txt")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("/absolute.txt")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("safe/relative.txt")).isFalse();
    }

    @Test
    void hasUnsafeHierarchicalPath_shouldRejectMalformedPath() {
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("bad\u0000name.txt")).isTrue();
    }

    @Test
    void hasUnsafeHierarchicalPath_shouldRejectWindowsDriveAndUncPaths() {
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("C:/Windows/win.ini")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("C:\\Windows\\win.ini")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("\\\\server\\share\\file.txt")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("//server/share/file.txt")).isTrue();
    }

    @Test
    void validateZipSlip_shouldThrow_whenEntryEscapesTargetDirectory() {
        Path target = Path.of("/tmp/safe");
        Path entry = target.resolve("../../outside.txt");

        assertThatThrownBy(() -> ZipEntryPaths.validateZipSlip(entry, target))
            .isInstanceOf(SafeZipException.class)
            .hasMessageContaining("Zip slip detected");
    }

    @Test
    void validateZipSlip_shouldAllowEntryInsideTargetDirectory() {
        Path target = Path.of("/tmp/safe").toAbsolutePath();
        Path entry = target.resolve("nested/file.txt");

        ZipEntryPaths.validateZipSlip(entry, target);
    }
}
