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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;

final class ZipEntryPaths {

    private ZipEntryPaths() {}

    static String normalizeEntryName(String name) {
        return name.replace('\\', '/');
    }

    static boolean isDirectoryEntry(String name, boolean zipDirectoryFlag) {
        return zipDirectoryFlag || normalizeEntryName(name).endsWith("/");
    }

    static String getFileName(String entryName, boolean directory) {
        String normalized = normalizeEntryName(entryName);
        if (directory && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        Path path = Path.of(normalized);
        Path fileName = path.getFileName();
        return fileName == null ? normalized : fileName.toString();
    }

    static Optional<String> getFolderName(String entryName, boolean directory, int level) {
        String normalized = normalizeEntryName(entryName);
        if (directory && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        Path path = Path.of(normalized);
        int nameCount = path.getNameCount();
        int folderSegments = directory ? nameCount : nameCount - 1;
        if (level < 0 || level >= folderSegments) {
            return Optional.empty();
        }
        return Optional.of(path.getName(level).toString());
    }

    static String stripTrailingSeparator(String normalized) {
        String result = normalized;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    static boolean hasUnsafeFlatPath(String name) {
        String normalized = normalizeEntryName(name);
        if (normalized.contains("/")) {
            return true;
        }
        if (hasParentTraversalSegment(normalized)) {
            return true;
        }
        if (normalized.length() >= 2 && normalized.charAt(1) == ':' && Character.isLetter(normalized.charAt(0))) {
            return true;
        }
        try {
            Path.of(normalized);
            return false;
        } catch (InvalidPathException ignored) {
            return true;
        }
    }

    @SuppressWarnings("java:S7467")
    static boolean hasUnsafeHierarchicalPath(String name) {
        String normalized = normalizeEntryName(name);
        if (hasParentTraversalSegment(normalized)) {
            return true;
        }
        if (normalized.startsWith("/") || normalized.startsWith("//")) {
            return true;
        }
        if (normalized.length() >= 2 && normalized.charAt(1) == ':' && Character.isLetter(normalized.charAt(0))) {
            return true;
        }
        try {
            return Path.of(normalized).normalize().toString().startsWith("..");
        } catch (InvalidPathException ignored) {
            return true;
        }
    }

    private static boolean hasParentTraversalSegment(String normalized) {
        if (normalized.isEmpty()) {
            return false;
        }
        for (String segment : normalized.split("/")) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    static Path resolveTargetRoot(Path targetDirectory) throws IOException {
        Path target = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(target);
        if (Files.isSymbolicLink(target)) {
            throw new IOException(
                MessageFormat.format("Extraction target directory must not be a symbolic link: {0}", target)
            );
        }
        return target.toRealPath();
    }

    static Path resolveEntryPath(Path targetRoot, String entryName) throws IOException {
        try {
            String normalized = normalizeEntryName(entryName);
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            if (normalized.isEmpty()) {
                throw new IOException(MessageFormat.format("The archive contains an unsafe path: {0}", entryName));
            }
            Path entryPath = targetRoot.resolve(normalized).normalize();
            if (!entryPath.startsWith(targetRoot)) {
                throw zipSlipIOException(entryName, entryPath);
            }
            return entryPath;
        } catch (InvalidPathException e) {
            throw zipSlipIOException(entryName, e);
        }
    }

    static void ensurePathWithinTarget(Path path, Path targetRoot) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        if (!Files.exists(absolute)) {
            Path parent = absolute.getParent();
            if (parent != null) {
                ensurePathWithinTarget(parent, targetRoot);
            }
            return;
        }
        if (Files.isSymbolicLink(absolute)) {
            throw zipSlipIOException(absolute);
        }
        Path real = absolute.toRealPath();
        if (!real.startsWith(targetRoot)) {
            throw zipSlipIOException(absolute);
        }
    }

    static void validateZipSlip(Path entryPath, Path targetDirectory) throws IOException {
        if (!entryPath.normalize().startsWith(targetDirectory.normalize())) {
            throw zipSlipIOException(entryPath);
        }
    }

    private static IOException zipSlipIOException(String entryName, Path entryPath) {
        return new IOException(
            MessageFormat.format("Zip slip detected: invalid entry path: {0} (resolved: {1})", entryName, entryPath)
        );
    }

    private static IOException zipSlipIOException(String entryName, InvalidPathException cause) {
        return new IOException(MessageFormat.format("Zip slip detected: invalid entry path: {0}", entryName), cause);
    }

    private static IOException zipSlipIOException(Path resolvedPath) {
        return new IOException(
            MessageFormat.format("Zip slip detected: invalid entry path (resolved: {0})", resolvedPath)
        );
    }
}
