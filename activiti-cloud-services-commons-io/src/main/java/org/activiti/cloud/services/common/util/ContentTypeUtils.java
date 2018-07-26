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

import static org.springframework.boot.web.server.MimeMappings.DEFAULT;

/**
 * Utils for handling content type
 */
public final class ContentTypeUtils {

    public static final String CONTENT_TYPE_JSON = DEFAULT.get("json");

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

    private ContentTypeUtils() {

    }
}
