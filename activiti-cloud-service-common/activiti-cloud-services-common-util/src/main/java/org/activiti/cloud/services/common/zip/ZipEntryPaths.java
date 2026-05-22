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
import java.nio.file.Path;
import java.util.Optional;

final class ZipEntryPaths {

    private ZipEntryPaths() {}

    static String normalizeEntryName(String name) {
        return name.replace('\\', '/');
    }

    static boolean isDirectoryEntry(String name, boolean zipDirectoryFlag) {
        return zipDirectoryFlag || name.endsWith("/");
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

    static boolean hasUnsafeFlatPath(String name) {
        return name.contains("/") || name.contains("\\") || name.contains("..");
    }

    static boolean hasUnsafeHierarchicalPath(String name) {
        String normalized = normalizeEntryName(name);
        if (normalized.contains("..")) {
            return true;
        }
        if (normalized.startsWith("/") || normalized.startsWith("//")) {
            return true;
        }
        if (normalized.length() >= 2 && normalized.charAt(1) == ':' && Character.isLetter(normalized.charAt(0))) {
            return true;
        }
        return Path.of(normalized).normalize().toString().startsWith("..");
    }

    static Path resolveTargetRoot(Path targetDirectory) throws IOException {
        Path target = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(target);
        return target.toRealPath();
    }

    static Path resolveEntryPath(Path targetRoot, String entryName) {
        String normalized = normalizeEntryName(entryName);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        Path entryPath = targetRoot.resolve(normalized).normalize();
        if (!entryPath.startsWith(targetRoot)) {
            throw new SafeZipException("Zip slip detected: invalid entry path");
        }
        return entryPath;
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
            throw new SafeZipException("Zip slip detected: invalid entry path");
        }
        Path real = absolute.toRealPath();
        if (!real.startsWith(targetRoot)) {
            throw new SafeZipException("Zip slip detected: invalid entry path");
        }
    }

    static void validateZipSlip(Path entryPath, Path targetDirectory) {
        if (!entryPath.normalize().startsWith(targetDirectory.normalize())) {
            throw new SafeZipException("Zip slip detected: invalid entry path");
        }
    }
}
