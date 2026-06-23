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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class SafeZipExtractorTest {

    private static final long MB = 1024L * 1024L;

    @Test
    void extractEntries_shouldThrow_whenArchiveIsEmpty() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes();
        SafeZipLimits limits = modelImportLimits();

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("archive is empty");
    }

    @Test
    void forEachEntry_shouldNotInvokeConsumer_whenArchiveIsEmpty() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes();
        List<SafeZipEntry> collected = new ArrayList<>();

        SafeZipExtractor.forEachEntry(new ByteArrayInputStream(zip), modelImportLimits(), collected::add);

        assertThat(collected).isEmpty();
    }

    @Test
    void extractEntries_shouldThrow_whenArchiveHasMoreThanMaxEntries() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.entry("a.json", "{}"),
            ZipTestFixtures.entry("b.json", "{}"),
            ZipTestFixtures.entry("c.json", "{}")
        );
        SafeZipLimits limits = modelImportLimits();

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("at most 2 entries");
    }

    @Test
    void extractEntries_shouldThrow_whenArchiveContainsDirectory() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.directory("folder/"));
        SafeZipLimits limits = modelImportLimits();

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("must not contain folders or empty entries");
    }

    @Test
    void extractEntries_shouldRejectDirectoryWithHiddenPayload() throws IOException {
        byte[] payload = ZipTestFixtures.incompressibleBytes(1024 * 1024);
        byte[] zip = ZipTestFixtures.zipBytesWithDirectoryPayload("trap/", payload);
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(512 * 1024)
            .maxTotalDecompressedBytes(MB)
            .allowDirectories(true)
            .flatEntryPaths(false)
            .build();
        InputStream inputStream = new ByteArrayInputStream(zip);

        assertThatThrownBy(() -> SafeZipExtractor.extractEntries(inputStream, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("archive entry is too large");
    }

    @Test
    void extractEntries_shouldAllowZipBuilderLayout_whenHierarchicalPathsAllowed() throws IOException {
        byte[] zip = new ZipBuilder("import")
            .appendFolder("assets")
            .appendFile("payload".getBytes(UTF_8), "assets", "file.json")
            .toZipBytes();
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .allowDirectories(true)
            .flatEntryPaths(false)
            .build();

        var entries = SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits);

        assertThat(entries)
            .hasSize(2)
            .anyMatch(SafeZipEntry::directory)
            .anyMatch(entry -> entry.name().equals("assets/file.json"));
    }

    @Test
    void extractEntries_shouldAllowDirectory_whenConfigured() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.directory("folder/"),
            ZipTestFixtures.entry("folder/file.json", "{}")
        );
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .allowDirectories(true)
            .flatEntryPaths(false)
            .allowedExtensions(Set.of("json"))
            .build();

        var entries = SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits);

        assertThat(entries).hasSize(2).anyMatch(SafeZipEntry::directory);
    }

    @Test
    void extractEntries_shouldThrow_whenEntryContainsForwardSlash() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("folder/file.json", "{}"));

        SafeZipLimits limits = modelImportLimits();
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unsafe path");
    }

    @Test
    void extractEntries_shouldThrow_whenHierarchicalPathIsUnsafe() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("../evil.json", "{}"));
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .flatEntryPaths(false)
            .allowedExtensions(Set.of("json"))
            .build();

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unsafe path");
    }

    @Test
    void extractEntries_shouldThrow_whenHierarchicalPathUsesWindowsDriveLetter() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("C:/Windows/evil.json", "{}"));
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .flatEntryPaths(false)
            .allowedExtensions(Set.of("json"))
            .build();

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unsafe path");
    }

    @Test
    void extractEntries_shouldAcceptJson_whenAllowedExtensionsUseUpperCase() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("file.json", "{}"));
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .allowedExtensions(Set.of("JSON"))
            .build();

        var entries = SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits);

        assertThat(entries).hasSize(1);
    }

    @Test
    void extractEntries_shouldThrow_whenEntryContainsBackslash() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("folder\\file.json", "{}"));

        SafeZipLimits limits = modelImportLimits();
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unsafe path");
    }

    @Test
    void extractEntries_shouldThrow_whenEntryContainsParentTraversal() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("..", "{}"));

        SafeZipLimits limits = modelImportLimits();
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unsafe path");
    }

    @Test
    void extractEntries_shouldThrow_whenEntryIsNotJson() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("malware.exe", "{}"));

        SafeZipLimits limits = modelImportLimits();
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("File extension not allowed");
    }

    @Test
    void extractEntries_shouldAllowAnyExtension_whenFilterDisabled() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("data.bin", "ok"));
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .build();

        var entries = SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits);

        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().name()).isEqualTo("data.bin");
    }

    @Test
    void extractEntries_shouldThrow_whenEntryIsExecutable() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("my-connector.json", "{}"));
        SafeZipLimits limits = modelImportLimits(bytes -> true);

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("executable file");
    }

    @Test
    void extractEntries_shouldThrow_whenArchiveCannotBeRead() throws IOException {
        byte[] validZip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("my-connector.json", "{}"));
        InputStream failingStream = new FilterInputStream(new ByteArrayInputStream(validZip)) {
            private boolean firstRead = true;

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                if (firstRead) {
                    firstRead = false;
                    return super.read(buffer, offset, length);
                }
                throw new IOException("simulated read failure");
            }
        };
        SafeZipLimits limits = modelImportLimits();

        assertThatThrownBy(() -> extractEntries(failingStream, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("simulated read failure");
    }

    @Test
    void extractEntries_shouldThrow_whenEntryExceedsMaxSize() throws IOException {
        byte[] oversized = ZipTestFixtures.incompressibleBytes(10 * 1024 * 1024 + 1);
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("big.json", oversized));

        SafeZipLimits limits = modelImportLimits();
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("archive entry is too large");
    }

    @Test
    void extractEntries_shouldRejectZipBomb_when_compressionRatioExceedsCap() throws IOException {
        byte[] payload = new byte[1024 * 1024];
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("bomb.json", payload));

        SafeZipLimits limits = modelImportLimits();

        assertThat(zip).hasSizeLessThan(10 * 1024);
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("suspiciously high compression ratio");
    }

    @Test
    void extractEntries_shouldRejectZipBomb_when_compressionRatioIsCheckedDespiteSmallCompressedSize()
        throws IOException {
        byte[] payload = new byte[64 * 1024];
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("bomb.json", payload));

        SafeZipLimits limits = modelImportLimits();

        assertThat(zip).hasSizeLessThan(512);
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("suspiciously high compression ratio");
    }

    @Test
    void extractEntries_shouldThrow_whenEntryExceedsLimitDuringRead() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("big.json", new byte[64]));
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(16)
            .maxTotalDecompressedBytes(1024)
            .build();

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("archive entry is too large");
    }

    @Test
    void extractEntries_shouldThrow_whenEntryIsEmpty() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("empty.json", ""));

        SafeZipLimits limits = modelImportLimits();
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("must not contain folders or empty entries");
    }

    @Test
    void extractEntries_shouldReturnEmptyEntry_whenEmptyEntriesNotRejected() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.entry("empty.json", ""),
            ZipTestFixtures.entry("data.json", "{}")
        );

        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .build();

        var entries = SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits);

        assertThat(entries).hasSize(2);
        assertThat(entries.getFirst().name()).isEqualTo("empty.json");
        assertThat(entries.getFirst().content()).isEmpty();
        assertThat(entries.getFirst().directory()).isFalse();
    }

    @Test
    void extractEntries_shouldThrow_when_totalDecompressedExceedsMaxSize() throws IOException {
        byte[] halfLimitPlusOne = ZipTestFixtures.incompressibleBytes(5 * 1024 * 1024 + 1);
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.entry("a.json", halfLimitPlusOne),
            ZipTestFixtures.entry("b.json", halfLimitPlusOne)
        );

        SafeZipLimits limits = modelImportLimits();
        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("decompresses to too much data");
    }

    @Test
    void extractEntries_shouldThrow_whenNestedZipDetected() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.entry("nested.zip", ZipTestFixtures.zipLocalFileHeaderBytes())
        );
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .rejectNestedZipEntries(true)
            .build();

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("nested zip file");
    }

    @Test
    void extractEntries_shouldNotTreatShortPayloadAsNestedZip() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("tiny.json", new byte[] { 0x50, 0x4b }));

        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .rejectNestedZipEntries(true)
            .build();

        assertThat(SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits)).hasSize(1);
    }

    @Test
    void extractEntries_shouldNotTreatNonZipMagicAsNestedZip() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.entry("data.json", new byte[] { 0x00, 0x01, 0x02, 0x03 })
        );

        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .rejectNestedZipEntries(true)
            .build();

        assertThat(SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits)).hasSize(1);
    }

    @Test
    void extractEntries_shouldThrow_whenFileExtensionIsMissing() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("README", "text"));

        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .allowedExtensions(Set.of("json"))
            .build();

        assertThatThrownBy(() -> extractEntries(zip, limits))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("File extension not allowed");
    }

    @Test
    void extractEntries_shouldAllowNestedZip_whenExtensionIsPermitted() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.entry("bundle.zip", ZipTestFixtures.zipLocalFileHeaderBytes())
        );
        SafeZipLimits limits = SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(MB)
            .maxTotalDecompressedBytes(MB)
            .rejectNestedZipEntries(true)
            .nestedZipAllowedExtensions(Set.of("zip"))
            .build();

        var entries = SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits);

        assertThat(entries).hasSize(1);
    }

    @Test
    void extractEntries_shouldReturnEntries_whenArchiveIsValid() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("my-connector.json", "{\"name\":\"x\"}"));

        var entries = SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), modelImportLimits());

        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().name()).isEqualTo("my-connector.json");
        assertThat(entries.getFirst().content()).containsExactly("{\"name\":\"x\"}".getBytes(UTF_8));
        assertThat(entries.getFirst().directory()).isFalse();
    }

    @Test
    void extractEntries_shouldPreserveEntryContent() throws IOException {
        byte[] zip = zipWithStoredEntry("plain.json", "stored-content");

        var entries = SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), modelImportLimits());

        assertThat(new String(entries.getFirst().content(), UTF_8)).isEqualTo("stored-content");
    }

    static byte[] zipWithStoredEntry(String name, String content) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            ZipEntry entry = new ZipEntry(name);
            byte[] bytes = content.getBytes(UTF_8);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(bytes.length);
            entry.setCompressedSize(bytes.length);
            entry.setCrc(crc32(bytes));
            zos.putNextEntry(entry);
            zos.write(bytes);
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    private static long crc32(byte[] bytes) {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(bytes);
        return crc.getValue();
    }

    private static void extractEntries(byte[] zip, SafeZipLimits limits) throws IOException {
        SafeZipExtractor.extractEntries(new ByteArrayInputStream(zip), limits);
    }

    private static void extractEntries(InputStream stream, SafeZipLimits limits) throws IOException {
        SafeZipExtractor.extractEntries(stream, limits);
    }

    private static SafeZipLimits modelImportLimits() {
        return modelImportLimits(bytes -> false);
    }

    private static SafeZipLimits modelImportLimits(Predicate<byte[]> executableContentCheck) {
        return SafeZipLimits.builder()
            .maxEntries(2)
            .maxEntryDecompressedBytes(10 * MB)
            .maxTotalDecompressedBytes(10 * MB)
            .maxCompressionRatio(100)
            .allowDirectories(false)
            .flatEntryPaths(true)
            .allowedExtensions(Set.of("json"))
            .rejectEmptyEntries(true)
            .executableContentCheck(executableContentCheck)
            .build();
    }
}
