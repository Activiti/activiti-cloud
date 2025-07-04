/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.test.asserts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.activiti.cloud.services.common.file.FileContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssertZipContentTest {

    private FileContent fileContent;
    private AssertZipContent assertZipContent;

    @BeforeEach
    void setUp() throws IOException {
        fileContent = createZipFileContent();
        assertZipContent = new AssertZipContent(fileContent);
    }

    @Test
    void hasJsonContent_shouldSucceed_whenEntryContainsValidJson() {
        // Given a zip with valid JSON content
        // When asserting JSON content
        AssertZipContent result = assertZipContent.hasJsonContent("test.json");

        // Then assertion should pass and return fluent interface
        assertThat(result).isSameAs(assertZipContent);
    }

    @Test
    void hasJsonContent_shouldFail_whenEntryContainsInvalidJson() {
        // Given a zip with invalid JSON content
        // When asserting JSON content on invalid JSON
        // Then assertion should fail
        assertThatThrownBy(() -> assertZipContent.hasJsonContent("invalid.json")).isInstanceOf(AssertionError.class);
    }

    @Test
    void hasJsonContent_shouldFail_whenEntryDoesNotExist() {
        // Given a zip content
        // When asserting JSON content on non-existent entry
        // Then assertion should fail
        assertThatThrownBy(() -> assertZipContent.hasJsonContent("nonexistent.json"))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void hasJsonContentSatisfying_shouldSucceed_whenRequirementIsMet() {
        // Given a zip with valid JSON content
        // When asserting JSON content with custom requirement
        AssertZipContent result = assertZipContent.hasJsonContentSatisfying(
            "test.json",
            jsonAssert -> jsonAssert.inPath("$.name").isEqualTo("test")
        );

        // Then assertion should pass and return fluent interface
        assertThat(result).isSameAs(assertZipContent);
    }

    @Test
    void hasJsonContentSatisfying_shouldFail_whenRequirementIsNotMet() {
        // Given a zip with JSON content
        // When asserting JSON content with unmet requirement
        // Then assertion should fail
        assertThatThrownBy(() ->
                assertZipContent.hasJsonContentSatisfying(
                    "test.json",
                    jsonAssert -> jsonAssert.inPath("$.name").isEqualTo("wrong")
                )
            )
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void hasJsonContentSatisfying_shouldFail_whenEntryDoesNotExist() {
        // Given a zip content
        // When asserting JSON content with requirement on non-existent entry
        // Then assertion should fail
        assertThatThrownBy(() ->
                assertZipContent.hasJsonContentSatisfying("nonexistent.json", jsonAssert -> jsonAssert.isNotNull())
            )
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void hasJsonContentSatisfying_shouldFail_whenJsonIsInvalid() {
        // Given a zip with invalid JSON content
        // When asserting JSON content with requirement on invalid JSON
        // Then assertion should fail
        assertThatThrownBy(() ->
                assertZipContent.hasJsonContentSatisfying("invalid.json", jsonAssert -> jsonAssert.isNotNull())
            )
            .isInstanceOf(AssertionError.class);
    }

    private FileContent createZipFileContent() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add valid JSON entry
            ZipEntry jsonEntry = new ZipEntry("test.json");
            zos.putNextEntry(jsonEntry);
            zos.write("{\"name\": \"test\", \"value\": 123}".getBytes());
            zos.closeEntry();

            // Add invalid JSON entry
            ZipEntry invalidJsonEntry = new ZipEntry("invalid.json");
            zos.putNextEntry(invalidJsonEntry);
            zos.write("invalid json content".getBytes());
            zos.closeEntry();

            // Add text entry
            ZipEntry textEntry = new ZipEntry("test.txt");
            zos.putNextEntry(textEntry);
            zos.write("plain text content".getBytes());
            zos.closeEntry();
        }

        FileContent fileContent = mock(FileContent.class);
        when(fileContent.getFilename()).thenReturn("test.zip");
        when(fileContent.getContentType()).thenReturn("application/zip");
        when(fileContent.getFileContent()).thenReturn(baos.toByteArray());

        return fileContent;
    }
}
