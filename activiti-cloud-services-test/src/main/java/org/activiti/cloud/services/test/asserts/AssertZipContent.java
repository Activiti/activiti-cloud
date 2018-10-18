/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.test.asserts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.zip.ZipStream;

import static org.assertj.core.api.Assertions.*;

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
            ZipStream.of(inputStream).forEach(zipEntry -> {
                entries.add(zipEntry.getName());
                zipEntry.getContent()
                        .ifPresent(bytes -> contentMap.put(zipEntry.getName(),
                                                           bytes));
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
        assertThat(getEntries()).contains(entries);
        return this;
    }

    public AssertZipContent hasName(String name) {
        assertThat(getName()).isEqualTo(name);
        return this;
    }

    public AssertZipContent hasContent(String entry,
                                       byte[] content) {
        assertThat(getContentMap().get(entry)).isEqualTo(content);
        return this;
    }
}
