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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ZipTestFixtures {

    private ZipTestFixtures() {}

    static byte[] zipBytes(ZipEntrySpec... entries) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (ZipEntrySpec entry : entries) {
                zos.putNextEntry(new ZipEntry(entry.name()));
                if (entry.bytes() != null) {
                    zos.write(entry.bytes());
                }
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    static Path writeZipFile(Path directory, String fileName, ZipEntrySpec... entries) throws IOException {
        Path zipPath = directory.resolve(fileName);
        Files.write(zipPath, zipBytes(entries));
        return zipPath;
    }

    static byte[] incompressibleBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    static ZipEntrySpec entry(String name, String content) {
        return new ZipEntrySpec(name, content.getBytes(UTF_8));
    }

    static ZipEntrySpec entry(String name, byte[] content) {
        return new ZipEntrySpec(name, content);
    }

    static ZipEntrySpec directory(String name) {
        return new ZipEntrySpec(name.endsWith("/") ? name : name + "/", null);
    }

    static byte[] zipBytesWithDirectoryPayload(String directoryName, byte[] payload) throws IOException {
        String dirName = directoryName.endsWith("/") ? directoryName : directoryName + "/";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            ZipEntry entry = new ZipEntry(dirName);
            zos.putNextEntry(entry);
            zos.write(payload);
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    static byte[] zipLocalFileHeaderBytes() {
        return new byte[] { 0x50, 0x4B, 0x03, 0x04 };
    }

    record ZipEntrySpec(String name, byte[] bytes) {}
}
