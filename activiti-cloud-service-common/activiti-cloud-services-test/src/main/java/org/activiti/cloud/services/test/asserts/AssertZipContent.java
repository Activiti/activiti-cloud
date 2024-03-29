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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert.ConfigurableJsonFluentAssert;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.zip.ZipStream;

/**
 * Asserts for zip content
 */
public class AssertZipContent {

    private final String name;

    private final String contentType;

    private final List<String> entries = new ArrayList<>();

    private final Map<String, byte[]> contentMap = new HashMap<>();

    public AssertZipContent(FileContent fileContent) throws IOException {
        this.name = fileContent.getFilename();
        this.contentType = fileContent.getContentType();
        try (InputStream inputStream = new ByteArrayInputStream(fileContent.getFileContent())) {
            ZipStream
                .of(inputStream)
                .forEach(zipEntry -> {
                    entries.add(zipEntry.getName());
                    zipEntry.getContent().ifPresent(bytes -> contentMap.put(zipEntry.getName(), bytes));
                });
        }
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public List<String> getEntries() {
        return entries;
    }

    public Map<String, byte[]> getContentMap() {
        return contentMap;
    }

    public AssertZipContent hasEntries(String... entries) {
        assertThat(getEntries()).containsExactlyInAnyOrder(entries);
        return this;
    }

    public AssertZipContent hasName(String name) {
        assertThat(getName()).isEqualTo(name);
        return this;
    }

    public AssertZipContent hasContent(String entry, byte[] expectedContent) {
        hasContent(entry, new String(expectedContent));
        return this;
    }

    public AssertZipContent hasContent(String entry, String expectedContent) {
        hasContentSatisfying(
            entry,
            actualContent -> {
                assertThat(actualContent).isEqualTo(expectedContent);
            }
        );
        return this;
    }

    public AssertZipContent hasContentSatisfying(String entry, Consumer<String> requirement) {
        assertThat(zipContent(entry)).hasValueSatisfying(requirement);
        return this;
    }

    public AssertZipContent hasJsonContent(String entry) {
        assertThat(zipContent(entry)).hasValueSatisfying(JsonFluentAssert::assertThatJson);
        return this;
    }

    public AssertZipContent hasJsonContentSatisfying(String entry, Consumer<ConfigurableJsonFluentAssert> requirement) {
        assertThat(zipContent(entry)).map(JsonFluentAssert::assertThatJson).hasValueSatisfying(requirement);
        return this;
    }

    private Optional<String> zipContent(String entry) {
        return Optional.ofNullable(entry).map(contentMap::get).map(String::new);
    }
}
