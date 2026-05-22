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

class SafeZipExtractorDiskTest {

    private static final long KB = 1024L;

    @TempDir
    Path tempDir;

    @Test
    void extractToDirectory_shouldExtractFiles_whenArchiveIsValid() throws IOException {
        Path zipPath = ZipTestFixtures.writeZipFile(
            tempDir,
            "valid.zip",
            ZipTestFixtures.entry("nested/file.txt", "payload")
        );
        Path target = tempDir.resolve("out");

        SafeZipExtractor.extractToDirectory(zipPath.toFile(), target, permissiveLimits());

        assertThat(Files.readString(target.resolve("nested/file.txt"))).isEqualTo("payload");
    }

    @Test
    void extractToDirectory_shouldCreateDirectoryEntries_whenAllowed() throws IOException {
        Path zipPath = ZipTestFixtures.writeZipFile(
            tempDir,
            "dirs.zip",
            ZipTestFixtures.directory("empty/"),
            ZipTestFixtures.entry("empty/file.txt", "x")
        );
        Path target = tempDir.resolve("out");

        SafeZipExtractor.extractToDirectory(zipPath.toFile(), target, permissiveLimits());

        assertThat(Files.isDirectory(target.resolve("empty"))).isTrue();
        assertThat(Files.readString(target.resolve("empty/file.txt"))).isEqualTo("x");
    }

    @Test
    void extractToDirectory_shouldSkipDirectoryEntries_whenNotAllowed() throws IOException {
        Path zipPath = ZipTestFixtures.writeZipFile(
            tempDir,
            "dirs.zip",
            ZipTestFixtures.directory("ignored/"),
            ZipTestFixtures.entry("file.txt", "only-file")
        );
        Path target = tempDir.resolve("out");

        SafeZipLimits limits = SafeZipLimits
            .builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(1024 * KB)
            .maxTotalDecompressedBytes(1024 * KB)
            .allowDirectories(false)
            .build();

        SafeZipExtractor.extractToDirectory(zipPath.toFile(), target, limits);

        assertThat(Files.exists(target.resolve("ignored"))).isFalse();
        assertThat(Files.readString(target.resolve("file.txt"))).isEqualTo("only-file");
    }

    @Test
    void extractToDirectory_shouldThrow_whenZipSlipDetected() throws IOException {
        Path zipPath = ZipTestFixtures.writeZipFile(
            tempDir,
            "slip.zip",
            ZipTestFixtures.entry("../../outside.txt", "evil")
        );
        Path target = tempDir.resolve("out");

        assertThatThrownBy(() -> SafeZipExtractor.extractToDirectory(zipPath.toFile(), target, permissiveLimits()))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip slip detected")
            .hasCauseInstanceOf(SafeZipException.class);
    }

    @Test
    void extractToDirectory_shouldThrow_whenTooManyEntries() throws IOException {
        Path zipPath = ZipTestFixtures.writeZipFile(
            tempDir,
            "many.zip",
            ZipTestFixtures.entry("a.txt", "1"),
            ZipTestFixtures.entry("b.txt", "2"),
            ZipTestFixtures.entry("c.txt", "3")
        );
        SafeZipLimits limits = SafeZipLimits
            .builder()
            .maxEntries(2)
            .maxEntryDecompressedBytes(1024)
            .maxTotalDecompressedBytes(2048)
            .build();

        assertThatThrownBy(() -> SafeZipExtractor.extractToDirectory(zipPath.toFile(), tempDir.resolve("out"), limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Total number of ZIP entries exceeds maximum allowed");
    }

    @Test
    void extractToDirectory_shouldThrow_whenEntryMetadataExceedsMaxSize() throws IOException {
        byte[] payload = new byte[1024];
        Path zipPath = ZipTestFixtures.writeZipFile(tempDir, "big-meta.zip", ZipTestFixtures.entry("big.txt", payload));
        SafeZipLimits limits = SafeZipLimits
            .builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(1)
            .maxTotalDecompressedBytes(2048)
            .build();

        assertThatThrownBy(() -> SafeZipExtractor.extractToDirectory(zipPath.toFile(), tempDir.resolve("out"), limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Entry size exceeds maximum allowed");
    }

    @Test
    void extractToDirectory_shouldThrow_whenTotalExtractedSizeExceedsLimit() throws IOException {
        Path zipPath = ZipTestFixtures.writeZipFile(
            tempDir,
            "total.zip",
            ZipTestFixtures.entry("a.txt", "aaaa"),
            ZipTestFixtures.entry("b.txt", "bbbb")
        );
        SafeZipLimits limits = SafeZipLimits
            .builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(1024)
            .maxTotalDecompressedBytes(6)
            .build();

        assertThatThrownBy(() -> SafeZipExtractor.extractToDirectory(zipPath.toFile(), tempDir.resolve("out"), limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Total extraction size exceeds maximum allowed");
    }

    @Test
    void extractToDirectory_shouldThrow_whenParentDirectoryIsSymlinkOutsideTarget() throws IOException {
        Path outside = tempDir.resolve("outside");
        Files.createDirectories(outside);
        Path target = tempDir.resolve("out");
        Files.createDirectories(target);
        Path trap = target.resolve("trap");
        try {
            Files.createSymbolicLink(trap, outside);
        } catch (UnsupportedOperationException | IOException e) {
            org.junit.jupiter.api.Assumptions.abort("Symbolic links are not supported in this environment");
        }

        Path zipPath = ZipTestFixtures.writeZipFile(
            tempDir,
            "symlink.zip",
            ZipTestFixtures.entry("trap/evil.txt", "payload")
        );

        assertThatThrownBy(() -> SafeZipExtractor.extractToDirectory(zipPath.toFile(), target, permissiveLimits()))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip slip detected");

        assertThat(Files.exists(outside.resolve("evil.txt"))).isFalse();
    }

    @Test
    void extractToDirectory_shouldThrow_whenEntryUsesWindowsDrivePath() throws IOException {
        Path zipPath = ZipTestFixtures.writeZipFile(
            tempDir,
            "windows.zip",
            ZipTestFixtures.entry("C:/Windows/evil.txt", "x")
        );
        SafeZipLimits limits = SafeZipLimits
            .builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(1024)
            .maxTotalDecompressedBytes(2048)
            .flatEntryPaths(false)
            .build();

        assertThatThrownBy(() -> SafeZipExtractor.extractToDirectory(zipPath.toFile(), tempDir.resolve("out"), limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unsafe path")
            .hasCauseInstanceOf(SafeZipException.class);
    }

    @Test
    void extractToDirectory_shouldThrow_whenHierarchicalPathIsUnsafe() throws IOException {
        Path zipPath = ZipTestFixtures.writeZipFile(tempDir, "unsafe.zip", ZipTestFixtures.entry("../evil.txt", "x"));
        SafeZipLimits limits = SafeZipLimits
            .builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(1024)
            .maxTotalDecompressedBytes(2048)
            .flatEntryPaths(false)
            .build();

        assertThatThrownBy(() -> SafeZipExtractor.extractToDirectory(zipPath.toFile(), tempDir.resolve("out"), limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip slip detected")
            .hasCauseInstanceOf(SafeZipException.class);
    }

    private static SafeZipLimits permissiveLimits() {
        return SafeZipLimits
            .builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(1024 * KB)
            .maxTotalDecompressedBytes(1024 * KB)
            .build();
    }
}
