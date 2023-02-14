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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.activiti.cloud.services.common.util.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class FileContentSanitizerTest {

    private static final String SVG_FILE_LOCATION = "images/save.svg";

    private static final String SVG_IMAGE_PREFIX = "data:image/svg+xml;base64,";

    private final FileContentSanitizer fileContentSanitizer = new FileContentSanitizer();

    @Test
    void should_sanitizeSvgFileContent() throws IOException {
        byte[] svg = FileUtils.resourceAsStream(SVG_FILE_LOCATION)
                .orElseThrow(() -> new IllegalArgumentException(SVG_FILE_LOCATION + " file not found"))
                .readAllBytes();

        FileContent sanitizedContent = fileContentSanitizer.sanitizeContent(
                new FileContent("img.svg", ContentTypeUtils.CONTENT_TYPE_SVG, svg));

        Assertions.assertThat(sanitizedContent.getFilename()).isEqualTo("img.png");
        Assertions.assertThat(sanitizedContent.getContentType()).isEqualTo(ContentTypeUtils.CONTENT_TYPE_PNG);
        Assertions.assertThat(sanitizedContent.toString()).contains("PNG");
    }

    @Test
    void should_sanitizeSvgInJsonFileContent() throws IOException {
        byte[] svg = FileUtils.resourceAsStream(SVG_FILE_LOCATION)
                .orElseThrow(() -> new IllegalArgumentException(SVG_FILE_LOCATION + " file not found"))
                .readAllBytes();
        String svgImageInBase64 = "{\"image\": \"" + SVG_IMAGE_PREFIX + Base64.getEncoder().encodeToString(svg) + "\"}";
        FileContent sanitizedContent = fileContentSanitizer.sanitizeContent(new FileContent("img.json",
                ContentTypeUtils.CONTENT_TYPE_JSON, svgImageInBase64.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(sanitizedContent.getFilename()).isEqualTo("img.json");
        Assertions.assertThat(sanitizedContent.getContentType()).isEqualTo(ContentTypeUtils.CONTENT_TYPE_JSON);
        Assertions.assertThat(sanitizedContent.toString()).contains("{\"image\":\"data:image/png;base64,");
    }
}
