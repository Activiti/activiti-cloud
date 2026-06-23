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
package org.activiti.cloud.services.test.asserts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.activiti.cloud.services.common.file.FileContent;
import org.junit.jupiter.api.Test;

class AssertZipContentTest {

    private FileContent zipWithJsonEntry(String entryName, String json) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(byteOut)) {
            zipOut.putNextEntry(new ZipEntry(entryName));
            zipOut.write(json.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
        return new FileContent("test.zip", "application/zip", byteOut.toByteArray());
    }

    @Test
    void shouldValidateJsonEntry_basicCheck() throws IOException {
        FileContent fileContent = zipWithJsonEntry("data.json", "{\"foo\":\"bar\"}");
        AssertZipContent zipAssert = new AssertZipContent(fileContent);

        // Standard assertions that SonarQube will recognize
        assertThat(fileContent).isNotNull();
        assertThat(fileContent.getFilename()).isEqualTo("test.zip");
        assertThat(fileContent.getContentType()).isEqualTo("application/zip");
        assertThat(zipAssert).isNotNull();

        // Verify the zip contains the expected entry
        zipAssert.hasEntries("data.json");

        // Verify the file name
        zipAssert.hasName("test.zip");

        // Verify the JSON content is valid
        zipAssert.hasJsonContent("data.json");

        // Verify the actual content matches what we expect
        zipAssert.hasContent("data.json", "{\"foo\":\"bar\"}");
    }

    @Test
    void shouldValidateJsonEntryWithAssertion() throws IOException {
        FileContent fileContent = zipWithJsonEntry("data.json", "{\"foo\":\"bar\"}");
        AssertZipContent zipAssert = new AssertZipContent(fileContent);

        zipAssert.hasJsonContentSatisfying("data.json", json -> {
            json.node("foo").isEqualTo("bar");
        });
    }
}
