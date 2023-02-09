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
package org.activiti.cloud.services.common.util;

import static java.util.Collections.singletonMap;
import static org.springframework.boot.web.server.MimeMappings.DEFAULT;

import java.util.Map;
import java.util.Optional;

/**
 * Utils for handling content type
 */
public final class ContentTypeUtils {

    public static final String JSON = "json";

    public static final String DMN = "dmn";

    public static final String XML = "xml";

    public static final String CONTENT_TYPE_JSON = DEFAULT.get(JSON);

    public static final String CONTENT_TYPE_XML = DEFAULT.get("xml");

    public static final String CONTENT_TYPE_SVG = "image/svg+xml";

    public static final String CONTENT_TYPE_PNG = "image/png";

    public static final String CONTENT_TYPE_ZIP = "application/zip";

    private static final String EMPTY_STRING = "";

    public static final char EXTENSION_SEPARATOR = '.';

    private static final char UNIX_SEPARATOR = '/';

    private static final char WINDOWS_SEPARATOR = '\\';

    private static final int NOT_FOUND = -1;

    public static final Map<String, String> CONTENT_TYPES = singletonMap(DMN, CONTENT_TYPE_XML);

    /**
     * Get the content type corresponding to an extension.
     *
     * @param extension the extension to search the content type for
     * @return the content type
     */
    public static Optional<String> getContentTypeByExtension(String extension) {
        return Optional.ofNullable(
            Optional.ofNullable(DEFAULT.get(extension))
                .orElseGet(() -> CONTENT_TYPES.get(extension)));
    }

    /**
     * Get the content type corresponding to a path.
     *
     * @param path the path to search the content type for
     * @return the content type
     */
    public static Optional<String> getContentTypeByPath(String path) {
        return getContentTypeByExtension(getExtension(path));
    }

    /**
     * Check if a content type is json
     *
     * @param contentType the content type to check
     * @return true if the given content type is json
     */
    public static boolean isJsonContentType(String contentType) {
        return CONTENT_TYPE_JSON.equals(contentType);
    }

    /**
     * Check if a content type is svg image
     *
     * @param contentType the content type to check
     * @return true if the given content type is svg
     */
    public static boolean isSvgContentType(String contentType) {
        return CONTENT_TYPE_SVG.equals(contentType);
    }

    public static String toJsonFilename(String filename) {
        return setExtension(filename,
            JSON);
    }

    public static String changeToJsonFilename(String filename) {
        return changeExtension(filename,
            JSON);
    }

    public static String setExtension(String filename,
        String extension) {

        if (filename == null) {
            return null;
        }

        return Optional.ofNullable(extension)
            .map(ContentTypeUtils::fullExtension)
            .filter(ext -> !filename.endsWith(ext))
            .map(fullExtension -> filename + fullExtension)
            .orElse(filename);
    }

    public static String changeExtension(String filename,
        String extension) {

        if (filename == null) {
            return null;
        }

        return Optional.ofNullable(extension)
            .map(ContentTypeUtils::fullExtension)
            .filter(ext -> !filename.endsWith(ext))
            .map(fullExtension -> removeExtension(filename) + fullExtension)
            .orElse(filename);
    }

    public static String removeExtension(String filename,
        String extension) {

        if (filename == null) {
            return null;
        }

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

    static String getExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        final int index = getIndexOfExtension(fileName);
        if (index == NOT_FOUND) {
            return EMPTY_STRING;
        }
        return fileName.substring(index + 1);
    }

    private static int getIndexOfExtension(String path) {

        int extensionPos = path.lastIndexOf(EXTENSION_SEPARATOR);
        int lastUnixPos = path.lastIndexOf(UNIX_SEPARATOR);
        int lastWindowsPos = path.lastIndexOf(WINDOWS_SEPARATOR);
        int lastSeparator = Math.max(lastUnixPos, lastWindowsPos);

        int index = lastSeparator > extensionPos ? NOT_FOUND : extensionPos;
        return index;
    }

    static String removeExtension(final String fileName) {

        if (fileName == null) {
            return null;
        }
        final int index = getIndexOfExtension(fileName);
        if (index == NOT_FOUND) {
            return fileName;
        }

        return fileName.substring(0, index);

    }

}
