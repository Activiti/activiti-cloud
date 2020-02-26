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

package org.activiti.cloud.services.common.util;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.activiti.cloud.services.common.file.FileContent;
import org.springframework.web.multipart.MultipartFile;

import static org.apache.commons.io.IOUtils.writeChunked;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.apache.commons.io.IOUtils.toByteArray;

/**
 * Utils for handling http request/response
 */
public final class HttpUtils {

    public static final String HEADER_ATTACHEMNT_FILENAME = "attachment;filename=";

    public static void writeFileToResponse(HttpServletResponse response,
                                           FileContent fileContent,
                                           boolean attachment) throws IOException {
        response.setContentType(fileContent.getContentType());
        if (attachment) {
            response.setHeader(CONTENT_DISPOSITION,
                               HEADER_ATTACHEMNT_FILENAME + fileContent.getFilename());
        }
        writeChunked(fileContent.getFileContent(),
                     response.getOutputStream());
    }

    public static FileContent multipartToFileContent(MultipartFile file) throws IOException {
        return new FileContent(file.getOriginalFilename(),
                               file.getContentType(),
                               toByteArray(file.getInputStream()));
    }

    private HttpUtils() {
    }
}
