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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    void getFileName_shouldStripTrailingSlash_forDirectoryEntry() {
        assertThat(ZipEntryPaths.getFileName("folder/", true)).isEqualTo("folder");
    }

    @Test
    void getFolderName_shouldReturnSegmentAtLevel() {
        assertThat(ZipEntryPaths.getFolderName("folder/nested/file.txt", false, 0)).contains("folder");
        assertThat(ZipEntryPaths.getFolderName("folder/nested/file.txt", false, 1)).contains("nested");
    }

    @Test
    void getFolderName_shouldReturnFolderSegment_forDirectoryEntry() {
        assertThat(ZipEntryPaths.getFolderName("folder/", true, 0)).contains("folder");
    }

    @Test
    void getFolderName_shouldReturnEmpty_whenLevelIsOutOfRange() {
        assertThat(ZipEntryPaths.getFolderName("folder/", true, 5)).isEmpty();
    }

    @Test
    void stripTrailingSeparator_shouldRemoveMultipleTrailingSlashes() {
        assertThat(ZipEntryPaths.stripTrailingSeparator("a/b///")).isEqualTo("a/b");
    }

    @Test
    void hasUnsafeFlatPath_shouldReturnFalse_forEmptyPath() {
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("")).isFalse();
    }

    @Test
    void hasUnsafeFlatPath_shouldReturnFalse_forSimpleFileName() {
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("file.json")).isFalse();
    }

    @Test
    void hasUnsafeFlatPath_shouldReturnTrue_forNestedPaths() {
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("folder/file.json")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("folder\\file.json")).isTrue();
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("nested/../file.json")).isTrue();
    }

    @Test
    void hasUnsafeFlatPath_shouldAllowDoubleDotsInFileName() {
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("report..final.json")).isFalse();
        assertThat(ZipEntryPaths.hasUnsafeFlatPath("v1..0.txt")).isFalse();
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
    void hasUnsafeHierarchicalPath_shouldAllowDoubleDotsInFileName() {
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("report..final.json")).isFalse();
        assertThat(ZipEntryPaths.hasUnsafeHierarchicalPath("folder/report..final.json")).isFalse();
    }

    @Test
    void resolveEntryPath_shouldRejectEmptyNormalizedName() {
        Path target = Path.of("/tmp/safe").toAbsolutePath();

        assertThatThrownBy(() -> ZipEntryPaths.resolveEntryPath(target, "/"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unsafe path");
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
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip slip detected");
    }

    @Test
    void validateZipSlip_shouldAllowEntryInsideTargetDirectory() throws IOException {
        Path target = Path.of("/tmp/safe").toAbsolutePath();
        Path entry = target.resolve("nested/file.txt");

        ZipEntryPaths.validateZipSlip(entry, target);
    }

    @Test
    void resolveEntryPath_shouldIncludeEntryName_whenZipSlipDetected(@TempDir Path tempDir) throws IOException {
        Path targetRoot = ZipEntryPaths.resolveTargetRoot(tempDir.resolve("out"));
        String entryName = "../../outside.txt";

        assertThatThrownBy(() -> ZipEntryPaths.resolveEntryPath(targetRoot, entryName))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip slip detected")
            .hasMessageContaining(entryName)
            .hasMessageContaining("resolved:");
    }

    @Test
    void resolveEntryPath_shouldIncludeEntryName_whenPathIsMalformed(@TempDir Path tempDir) throws IOException {
        Path targetRoot = ZipEntryPaths.resolveTargetRoot(tempDir.resolve("out"));
        String entryName = "bad\u0000name.txt";

        assertThatThrownBy(() -> ZipEntryPaths.resolveEntryPath(targetRoot, entryName))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip slip detected")
            .hasMessageContaining(entryName);
    }

    @Test
    void resolveTargetRoot_shouldRejectSymbolicLinkTarget(@TempDir Path tempDir) throws IOException {
        Path realDir = tempDir.resolve("real");
        Files.createDirectory(realDir);
        Path link = tempDir.resolve("link");
        try {
            Files.createSymbolicLink(link, realDir);
        } catch (UnsupportedOperationException | IOException _) {
            org.junit.jupiter.api.Assumptions.abort("Symbolic links are not supported in this environment");
        }

        assertThatThrownBy(() -> ZipEntryPaths.resolveTargetRoot(link))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("symbolic link")
            .hasMessageContaining(link.toString());
    }

    @Test
    void resolveTargetRoot_shouldReturnRealPath_forRegularDirectory(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("out");

        Path resolved = ZipEntryPaths.resolveTargetRoot(target);

        assertThat(resolved).isEqualTo(Files.createDirectories(target).toRealPath());
        assertThat(Files.isDirectory(resolved)).isTrue();
    }

    @Test
    void resolveEntryPath_shouldResolveEntry_whenNameHasTrailingSlash(@TempDir Path tempDir) throws IOException {
        Path targetRoot = ZipEntryPaths.resolveTargetRoot(tempDir.resolve("out"));

        Path resolved = ZipEntryPaths.resolveEntryPath(targetRoot, "nested/file.txt/");

        assertThat(resolved).isEqualTo(targetRoot.resolve("nested/file.txt"));
    }

    @Test
    void ensurePathWithinTarget_shouldThrow_whenExistingPathIsSymbolicLink(@TempDir Path tempDir) throws IOException {
        Path targetRoot = ZipEntryPaths.resolveTargetRoot(tempDir.resolve("out"));
        Path outside = tempDir.resolve("outside.txt");
        Files.writeString(outside, "secret");
        Path link = targetRoot.resolve("link.txt");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException _) {
            org.junit.jupiter.api.Assumptions.abort("Symbolic links are not supported in this environment");
        }

        assertThatThrownBy(() -> ZipEntryPaths.ensurePathWithinTarget(link, targetRoot))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip slip detected");
    }
}
