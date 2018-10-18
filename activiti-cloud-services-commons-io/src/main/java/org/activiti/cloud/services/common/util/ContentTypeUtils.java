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

import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.springframework.boot.web.server.MimeMappings.DEFAULT;

/**
 * Utils for handling content type
 */
public final class ContentTypeUtils {

    public static final String JSON = "json";

    public static final String CONTENT_TYPE_JSON = DEFAULT.get(JSON);

    public static final String CONTENT_TYPE_XML = DEFAULT.get("xml");

    public static final String CONTENT_TYPE_SVG = "image/svg+xml";

    public static final String CONTENT_TYPE_ZIP = "application/zip";

    /**
     * Get the content type corresponding to an extension.
     * @param extension the extension to search the content type for
     * @return the content type
     */
    public static Optional<String> getContentTypeByExtension(String extension) {
        return Optional.ofNullable(DEFAULT.get(extension));
    }

    /**
     * Get the content type corresponding to a path.
     * @param path the path to search the content type for
     * @return the content type
     */
    public static Optional<String> getContentTypeByPath(String path) {
        return getContentTypeByExtension(getExtension(path));
    }

    /**
     * Check if a content type is json
     * @param contentType the content type to check
     * @return true if the the given content type is json
     */
    public static boolean isJsonContentType(String contentType) {
        return CONTENT_TYPE_JSON.equals(contentType);
    }

    public static String toJsonFilename(String filename) {
        return setExtension(filename,
                            JSON);
    }

    public static String setExtension(String filename,
                                      String extension) {
        return Optional.ofNullable(extension)
                .map(ContentTypeUtils::fullExtension)
                .filter(ext -> !filename.endsWith(ext))
                .map(fullExtension -> FilenameUtils.removeExtension(filename) + fullExtension)
                .orElse(filename);
    }

    public static String removeExtension(String filename,
                                         String extension) {
        return Optional.ofNullable(extension)
                .map(ContentTypeUtils::fullExtension)
                .filter(filename::endsWith)
                .map(filename::lastIndexOf)
                .map(extensionIndex -> filename.substring(0,
                                                          extensionIndex))
                .orElse(filename);
    }

    public static String fullExtension(String extension) {
        return EXTENSION_SEPARATOR + extension;
    }

    private ContentTypeUtils() {

    }
}
