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

package org.activiti.cloud.services.modeling.service.utils;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Map;
import org.activiti.cloud.modeling.converter.StringSanitizingDeserializer;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.activiti.cloud.services.common.util.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileContentSanitizer {

    private final Logger log = LoggerFactory.getLogger(FileContentSanitizer.class);

    private final ObjectMapper objectMapper;

    private final PrettyPrinter prettyPrinter;

    public FileContentSanitizer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.prettyPrinter = new ModelExtensionsPrettyPrinter().withEmptyObjectSeparator('\0');

        SimpleModule module = new SimpleModule("jsonStringSanitizingModelingModule", Version.unknownVersion());
        module.addDeserializer(String.class, new StringSanitizingDeserializer());
        this.objectMapper.registerModule(module);
    }

    public FileContent sanitizeContent(FileContent fileContent) {
        try {
            byte[] updatedFileContent = fileContent.getFileContent();
            String updatedFileName = fileContent.getFilename();
            String updatedContentType = fileContent.getContentType();
            if (ContentTypeUtils.isSvgContentType(fileContent.getContentType())) {
                updatedFileContent = ImageUtils.svgToPng(fileContent.getFileContent());
                updatedFileName = ContentTypeUtils.changeExtension(updatedFileName, "png");
                updatedContentType = ContentTypeUtils.CONTENT_TYPE_PNG;
            } else if (ContentTypeUtils.isJsonContentType(fileContent.getContentType())) {
                // ObjectMapper sanitizes String values
                Map deserializedMap = objectMapper.readValue(fileContent.getFileContent(), Map.class);
                updatedFileContent = objectMapper.writer(prettyPrinter).writeValueAsBytes(deserializedMap);
            }
            return new FileContent(updatedFileName, updatedContentType, updatedFileContent);
        } catch (Exception e) {
            log.warn("Exception during file content sanitization", e);
        }
        return fileContent;
    }
}
