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

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SafeZipLimitsTest {

    @Test
    void build_shouldApplyDefaults_whenOptionalFieldsOmitted() {
        SafeZipLimits limits = minimalBuilder().build();

        assertThat(limits.maxCompressionRatio()).isEqualTo(100);
        assertThat(limits.allowDirectories()).isTrue();
        assertThat(limits.flatEntryPaths()).isFalse();
        assertThat(limits.rejectNestedZipEntries()).isFalse();
        assertThat(limits.rejectEmptyEntries()).isFalse();
        assertThat(limits.allowedExtensions()).isEmpty();
        assertThat(limits.nestedZipAllowedExtensions()).isEmpty();
        assertThat(limits.executableContentCheck().test(new byte[0])).isFalse();
    }

    @Test
    void build_shouldDefensivelyCopyExtensionSets() {
        Set<String> extensions = new HashSet<>(Set.of("json"));
        SafeZipLimits limits = minimalBuilder().allowedExtensions(extensions).build();
        extensions.add("xml");

        assertThat(limits.allowedExtensions()).containsExactly("json");
    }

    @Test
    void build_shouldTreatNullExtensionSetsAsEmpty() {
        SafeZipLimits limits = minimalBuilder().allowedExtensions(null).nestedZipAllowedExtensions(null).build();

        assertThat(limits.allowedExtensions()).isEmpty();
        assertThat(limits.nestedZipAllowedExtensions()).isEmpty();
    }

    @Test
    void build_shouldRejectZeroMaxEntries() {
        SafeZipLimits.Builder builder = minimalBuilder().maxEntries(0);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxEntries must be positive");
    }

    @Test
    void build_shouldRejectNegativeMaxEntries() {
        SafeZipLimits.Builder builder = minimalBuilder().maxEntries(-1);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxEntries must be positive");
    }

    @Test
    void build_shouldRejectZeroSizeLimits() {
        SafeZipLimits.Builder builder = minimalBuilder().maxEntryDecompressedBytes(0).maxTotalDecompressedBytes(1024);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size limits must be positive");
    }

    @Test
    void build_shouldRejectZeroMaxTotalDecompressedBytes() {
        SafeZipLimits.Builder builder = minimalBuilder().maxEntryDecompressedBytes(1024).maxTotalDecompressedBytes(0);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size limits must be positive");
    }

    @Test
    void build_shouldUseNoOpExecutableCheck_whenExecutableContentCheckIsNull() {
        SafeZipLimits limits = minimalBuilder().executableContentCheck(null).build();

        assertThat(limits.executableContentCheck().test(new byte[] { 0x7f })).isFalse();
    }

    @Test
    void build_shouldNormalizeExtensionSetsToLowerCase() {
        SafeZipLimits limits = minimalBuilder()
            .allowedExtensions(Set.of("JSON"))
            .nestedZipAllowedExtensions(Set.of("ZIP"))
            .build();

        assertThat(limits.allowedExtensions()).containsExactly("json");
        assertThat(limits.nestedZipAllowedExtensions()).containsExactly("zip");
    }

    @Test
    void build_shouldRejectInvalidCompressionRatio() {
        SafeZipLimits.Builder builder = minimalBuilder().maxCompressionRatio(0);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxCompressionRatio must be positive");
    }

    @Test
    void build_shouldRejectFlatEntryPathsWithAllowDirectories() {
        SafeZipLimits.Builder builder = minimalBuilder().allowDirectories(true).flatEntryPaths(true);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("allowDirectories cannot be combined with flatEntryPaths");
    }

    private static SafeZipLimits.Builder minimalBuilder() {
        return SafeZipLimits.builder().maxEntries(10).maxEntryDecompressedBytes(1024).maxTotalDecompressedBytes(2048);
    }
}
