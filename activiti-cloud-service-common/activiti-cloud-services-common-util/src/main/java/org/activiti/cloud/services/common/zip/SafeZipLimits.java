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

import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record SafeZipLimits(
    int maxEntries,
    long maxEntryDecompressedBytes,
    long maxTotalDecompressedBytes,
    long maxCompressionRatio,
    boolean allowDirectories,
    boolean flatEntryPaths,
    boolean rejectNestedZipEntries,
    Set<String> allowedExtensions,
    Set<String> nestedZipAllowedExtensions,
    Predicate<byte[]> executableContentCheck
) {
    public SafeZipLimits {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        if (maxEntryDecompressedBytes <= 0 || maxTotalDecompressedBytes <= 0) {
            throw new IllegalArgumentException("size limits must be positive");
        }
        if (maxCompressionRatio <= 0) {
            throw new IllegalArgumentException("maxCompressionRatio must be positive");
        }
        if (allowDirectories && flatEntryPaths) {
            throw new IllegalArgumentException("allowDirectories cannot be combined with flatEntryPaths");
        }
        allowedExtensions = normalizeExtensions(allowedExtensions);
        nestedZipAllowedExtensions = normalizeExtensions(nestedZipAllowedExtensions);
        executableContentCheck = executableContentCheck == null ? bytes -> false : executableContentCheck;
    }

    private static Set<String> normalizeExtensions(Set<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return Set.of();
        }
        return extensions
            .stream()
            .map(extension -> extension.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int maxEntries;
        private long maxEntryDecompressedBytes;
        private long maxTotalDecompressedBytes;
        private long maxCompressionRatio = 100;
        private boolean allowDirectories = true;
        private boolean flatEntryPaths = false;
        private boolean rejectNestedZipEntries = false;
        private Set<String> allowedExtensions = Set.of();
        private Set<String> nestedZipAllowedExtensions = Set.of();
        private Predicate<byte[]> executableContentCheck = bytes -> false;

        public Builder maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        public Builder maxEntryDecompressedBytes(long maxEntryDecompressedBytes) {
            this.maxEntryDecompressedBytes = maxEntryDecompressedBytes;
            return this;
        }

        public Builder maxTotalDecompressedBytes(long maxTotalDecompressedBytes) {
            this.maxTotalDecompressedBytes = maxTotalDecompressedBytes;
            return this;
        }

        public Builder maxCompressionRatio(long maxCompressionRatio) {
            this.maxCompressionRatio = maxCompressionRatio;
            return this;
        }

        public Builder allowDirectories(boolean allowDirectories) {
            this.allowDirectories = allowDirectories;
            return this;
        }

        public Builder flatEntryPaths(boolean flatEntryPaths) {
            this.flatEntryPaths = flatEntryPaths;
            return this;
        }

        public Builder rejectNestedZipEntries(boolean rejectNestedZipEntries) {
            this.rejectNestedZipEntries = rejectNestedZipEntries;
            return this;
        }

        public Builder allowedExtensions(Set<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
            return this;
        }

        public Builder nestedZipAllowedExtensions(Set<String> nestedZipAllowedExtensions) {
            this.nestedZipAllowedExtensions = nestedZipAllowedExtensions;
            return this;
        }

        public Builder executableContentCheck(Predicate<byte[]> executableContentCheck) {
            this.executableContentCheck = executableContentCheck;
            return this;
        }

        public SafeZipLimits build() {
            return new SafeZipLimits(
                maxEntries,
                maxEntryDecompressedBytes,
                maxTotalDecompressedBytes,
                maxCompressionRatio,
                allowDirectories,
                flatEntryPaths,
                rejectNestedZipEntries,
                allowedExtensions,
                nestedZipAllowedExtensions,
                executableContentCheck
            );
        }
    }
}
