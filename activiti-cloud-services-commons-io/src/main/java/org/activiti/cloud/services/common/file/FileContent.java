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

package org.activiti.cloud.services.common.file;

import java.util.Optional;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.isJsonContentType;

/**
 * Generic file content
 */
public class FileContent {

    private final String filename;

    private final String contentType;

    private final byte[] fileContent;

    public FileContent(String filename,
                       String contentType,
                       byte[] fileContent) {
        this.filename = filename;
        this.contentType = contentType;
        this.fileContent = fileContent;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public boolean isJson() {
        return isJsonContentType(contentType);
    }

    @Override
    public String toString() {
        return Optional.ofNullable(fileContent)
                .map(String::new)
                .orElse("");
    }
}
