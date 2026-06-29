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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class SafeZipStreamTest {

    @Test
    void forEach_shouldInvokeConsumer_forEachEntry() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.entry("a.txt", "one"),
            ZipTestFixtures.entry("b.txt", "two")
        );
        List<String> names = new ArrayList<>();

        SafeZipStream.of(new ByteArrayInputStream(zip), permissiveLimits()).forEach(entry ->
            names.add(entry.getName())
        );

        assertThat(names).containsExactly("a.txt", "b.txt");
    }

    @Test
    void ofMultipartFile_shouldDelegateToInputStream() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(
            ZipTestFixtures.entry("a.txt", "one"),
            ZipTestFixtures.entry("b.txt", "two")
        );
        List<String> names = new ArrayList<>();

        SafeZipStream.of(new BytesMultipartFile(zip), permissiveLimits()).forEach(entry -> names.add(entry.getName()));

        assertThat(names).containsExactly("a.txt", "b.txt");
    }

    @Test
    void forEach_shouldThrowIOException_whenPathIsUnsafe() throws IOException {
        byte[] zip = ZipTestFixtures.zipBytes(ZipTestFixtures.entry("../evil.txt", "x"));

        assertThatThrownBy(() -> SafeZipStream.of(new ByteArrayInputStream(zip), flatLimits()).forEach(entry -> {}))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("unsafe path");
    }

    private static SafeZipLimits permissiveLimits() {
        return SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(1024 * 1024)
            .maxTotalDecompressedBytes(1024 * 1024)
            .build();
    }

    private static SafeZipLimits flatLimits() {
        return SafeZipLimits.builder()
            .maxEntries(10)
            .maxEntryDecompressedBytes(1024)
            .maxTotalDecompressedBytes(2048)
            .allowDirectories(false)
            .flatEntryPaths(true)
            .build();
    }

    private static final class BytesMultipartFile implements MultipartFile {

        private final byte[] content;

        private BytesMultipartFile(byte[] content) {
            this.content = content;
        }

        @Override
        public String getName() {
            return "archive";
        }

        @Override
        public String getOriginalFilename() {
            return "archive.zip";
        }

        @Override
        public String getContentType() {
            return "application/zip";
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) {
            throw new UnsupportedOperationException();
        }
    }
}
