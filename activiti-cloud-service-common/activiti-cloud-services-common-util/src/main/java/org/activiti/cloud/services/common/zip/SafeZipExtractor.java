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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class SafeZipExtractor {

    private static final int READ_BUFFER_SIZE = 8 * 1024;
    private static final byte[] ZIP_MAGIC = new byte[] { 0x50, 0x4B, 0x03, 0x04 };

    private SafeZipExtractor() {}

    public static List<SafeZipEntry> extractEntries(InputStream zipInputStream, SafeZipLimits limits) {
        List<SafeZipEntry> entries = new ArrayList<>();
        forEachStreamEntry(zipInputStream, limits, entries::add);
        if (entries.isEmpty()) {
            throw new SafeZipException("The archive is empty");
        }
        return entries;
    }

    public static void forEachEntry(InputStream zipInputStream, SafeZipLimits limits, Consumer<SafeZipEntry> consumer) {
        forEachStreamEntry(zipInputStream, limits, consumer);
    }

    static void forEachStreamEntry(InputStream zipInputStream, SafeZipLimits limits, Consumer<SafeZipEntry> consumer) {
        long totalDecompressedSize = 0L;
        int entryCount = 0;

        CountingInputStream counting = new CountingInputStream(zipInputStream);
        try (ZipInputStream zis = new ZipInputStream(counting)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                entryCount = incrementAndValidateEntryCount(entryCount, limits.maxEntries());
                String name = zipEntry.getName();
                boolean directory = ZipEntryPaths.isDirectoryEntry(name, zipEntry.isDirectory());

                if (directory && !limits.allowDirectories()) {
                    throw new SafeZipException(
                        MessageFormat.format("The archive must not contain folders or empty entries: {0}", name)
                    );
                }

                validateEntryPath(name, limits, null);

                if (directory) {
                    consumer.accept(new SafeZipEntry(name, null, true));
                    continue;
                }

                validateExtension(name, limits);

                byte[] entryBytes = readEntryWithLimits(zis, counting, name, totalDecompressedSize, limits);
                if (entryBytes.length == 0) {
                    throw new SafeZipException(
                        MessageFormat.format("The archive must not contain folders or empty entries: {0}", name)
                    );
                }
                totalDecompressedSize += entryBytes.length;
                validateEntryContent(name, entryBytes, limits);
                consumer.accept(new SafeZipEntry(name, entryBytes, false));
            }
        } catch (IOException e) {
            throw new SafeZipException(MessageFormat.format("Cannot read archive: {0}", e.getMessage()), e);
        }
    }

    public static void extractToDirectory(File zipFile, Path targetDirectory, SafeZipLimits limits) throws IOException {
        try {
            extractToDirectoryInternal(zipFile, targetDirectory, limits);
        } catch (SafeZipException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static void extractToDirectoryInternal(File zipFile, Path targetDirectory, SafeZipLimits limits)
        throws IOException {
        Path targetRoot = ZipEntryPaths.resolveTargetRoot(targetDirectory);

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            long totalSize = 0L;
            int entryCount = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                entryCount = incrementAndValidateEntryCountForDisk(entryCount, limits.maxEntries());
                Path entryPath = ZipEntryPaths.resolveEntryPath(targetRoot, entry.getName());

                if (entry.isDirectory()) {
                    if (!limits.allowDirectories()) {
                        throw new SafeZipException(
                            MessageFormat.format(
                                "The archive must not contain folders or empty entries: {0}",
                                entry.getName()
                            )
                        );
                    }
                    validateEntryPath(entry.getName(), limits, targetRoot);
                    Files.createDirectories(entryPath);
                    ZipEntryPaths.ensurePathWithinTarget(entryPath, targetRoot);
                    continue;
                }

                validateEntryPath(entry.getName(), limits, targetRoot);
                validateExtension(entry.getName(), limits);
                validateEntrySizeMetadata(entry, limits);

                Path parentDir = entryPath.getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                    ZipEntryPaths.ensurePathWithinTarget(parentDir, targetRoot);
                }

                totalSize = extractFileEntry(zip, entry, entryPath, targetRoot, totalSize, limits);
            }
        }
    }

    private static void validateEntryPath(String name, SafeZipLimits limits, Path targetDirectory) {
        if (limits.flatEntryPaths()) {
            if (ZipEntryPaths.hasUnsafeFlatPath(name)) {
                throw new SafeZipException(MessageFormat.format("The archive contains an unsafe path: {0}", name));
            }
            return;
        }
        if (ZipEntryPaths.hasUnsafeHierarchicalPath(name)) {
            throw new SafeZipException(MessageFormat.format("The archive contains an unsafe path: {0}", name));
        }
        if (targetDirectory != null) {
            ZipEntryPaths.resolveEntryPath(targetDirectory, name);
        }
    }

    private static void validateExtension(String name, SafeZipLimits limits) {
        if (limits.allowedExtensions().isEmpty()) {
            return;
        }
        String extension = fileExtension(name);
        if (extension.isEmpty() || !limits.allowedExtensions().contains(extension)) {
            throw new SafeZipException(MessageFormat.format("File extension not allowed in the archive: {0}", name));
        }
    }

    private static void validateEntryContent(String name, byte[] entryBytes, SafeZipLimits limits) {
        if (limits.executableContentCheck().test(entryBytes)) {
            throw new SafeZipException(MessageFormat.format("The archive contains an executable file: {0}", name));
        }
        if (limits.rejectNestedZipEntries() && looksLikeZip(entryBytes)) {
            String extension = fileExtension(name);
            if (!limits.nestedZipAllowedExtensions().contains(extension)) {
                throw new SafeZipException(MessageFormat.format("The archive contains a nested zip file: {0}", name));
            }
        }
    }

    private static boolean looksLikeZip(byte[] bytes) {
        if (bytes.length < ZIP_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < ZIP_MAGIC.length; i++) {
            if (bytes[i] != ZIP_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static int incrementAndValidateEntryCount(int currentCount, int maxEntries) {
        int newCount = currentCount + 1;
        if (newCount > maxEntries) {
            throw new SafeZipException(
                MessageFormat.format("The archive must contain at most {0} entries", maxEntries)
            );
        }
        return newCount;
    }

    private static int incrementAndValidateEntryCountForDisk(int currentCount, int maxEntries) throws IOException {
        int newCount = currentCount + 1;
        if (newCount > maxEntries) {
            throw new IOException("Total number of ZIP entries exceeds maximum allowed");
        }
        return newCount;
    }

    private static void validateEntrySizeMetadata(ZipEntry entry, SafeZipLimits limits) throws IOException {
        long entrySize = entry.getSize();
        if (entrySize > limits.maxEntryDecompressedBytes()) {
            throw new IOException(
                "Entry size exceeds maximum allowed: " + entry.getName() + " (" + entrySize + " bytes)"
            );
        }
    }

    private static byte[] readEntryWithLimits(
        ZipInputStream zis,
        CountingInputStream counting,
        String name,
        long totalDecompressedSize,
        SafeZipLimits limits
    ) throws IOException {
        long entryCompressedStart = counting.getCount();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        long entryDecompressedSize = 0L;
        int read;
        while ((read = zis.read(buffer)) != -1) {
            entryDecompressedSize += read;
            if (entryDecompressedSize > limits.maxEntryDecompressedBytes()) {
                throw new SafeZipException(MessageFormat.format("The archive entry is too large: {0}", name));
            }
            if (totalDecompressedSize + entryDecompressedSize > limits.maxTotalDecompressedBytes()) {
                throw new SafeZipException("The archive decompresses to too much data");
            }
            validateCompressionRatio(
                entryDecompressedSize,
                counting.getCount() - entryCompressedStart,
                limits.maxCompressionRatio(),
                name
            );
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static long extractFileEntry(
        ZipFile zip,
        ZipEntry entry,
        Path entryPath,
        Path targetRoot,
        long totalSize,
        SafeZipLimits limits
    ) throws IOException {
        if (Files.isSymbolicLink(entryPath)) {
            throw new SafeZipException("Symlink entries are not allowed: " + entry.getName());
        }
        Path parent = entryPath.getParent();
        if (parent != null) {
            ZipEntryPaths.ensurePathWithinTarget(parent, targetRoot);
        }

        long compressedSize = entry.getCompressedSize();
        try (
            InputStream inputStream = zip.getInputStream(entry);
            OutputStream outputStream = new FileOutputStream(entryPath.toFile())
        ) {
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            long entryBytesRead = 0L;
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalSize = accumulateTotalSize(totalSize, bytesRead, limits.maxTotalDecompressedBytes());
                entryBytesRead =
                    accumulateEntrySize(entry.getName(), entryBytesRead, bytesRead, limits.maxEntryDecompressedBytes());
                if (compressedSize > 0) {
                    validateCompressionRatio(
                        entryBytesRead,
                        compressedSize,
                        limits.maxCompressionRatio(),
                        entry.getName()
                    );
                }
                outputStream.write(buffer, 0, bytesRead);
            }
            return totalSize;
        }
    }

    private static long accumulateTotalSize(long currentTotalSize, int bytesRead, long maxTotalSize)
        throws IOException {
        long newTotalSize = currentTotalSize + bytesRead;
        if (newTotalSize > maxTotalSize) {
            throw new IOException("Total extraction size exceeds maximum allowed");
        }
        return newTotalSize;
    }

    private static long accumulateEntrySize(String entryName, long currentEntryBytes, int bytesRead, long maxEntrySize)
        throws IOException {
        long newEntryBytes = currentEntryBytes + bytesRead;
        if (newEntryBytes > maxEntrySize) {
            throw new IOException("Entry size exceeds maximum allowed during extraction: " + entryName);
        }
        return newEntryBytes;
    }

    private static String fileExtension(String name) {
        int lastDot = name.lastIndexOf('.');
        int lastSeparator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastDot > lastSeparator && lastDot >= 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static final long MIN_COMPRESSED_BYTES_FOR_RATIO_CHECK = 512;

    private static void validateCompressionRatio(
        long decompressedSize,
        long compressedSize,
        long maxCompressionRatio,
        String entryName
    ) {
        if (
            compressedSize >= MIN_COMPRESSED_BYTES_FOR_RATIO_CHECK &&
            decompressedSize > compressedSize * maxCompressionRatio
        ) {
            throw new SafeZipException(
                MessageFormat.format(
                    "The archive entry has a suspiciously high compression ratio (possible ZIP bomb): {0}",
                    entryName
                )
            );
        }
    }
}
