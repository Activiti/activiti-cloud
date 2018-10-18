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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;

import static org.apache.commons.io.IOUtils.toByteArray;

/**
 * Utils for handling files
 */
public class FileUtils {

    /**
     * Get classpath resource as byte array
     * @param path the path of the resource
     * @return the byte array of the resource
     * @throws IOException if the resource cannot be read
     */
    public static byte[] resourceAsByteArray(String path) throws IOException {
        try (InputStream inputStream = new FileInputStream(new ClassPathResource(path).getFile())) {
            return toByteArray(inputStream);
        }
    }

    /**
     * Get classpath resource as file
     * @param name the name of the resource
     * @return the resource file, or {@link Optional#EMPTY}
     */
    public static Optional<File> resourceAsFile(String name) {
        ClassLoader classLoader = classLoader();
        return Optional.ofNullable(name)
                .map(classLoader::getResource)
                .map(URL::getFile)
                .map(File::new);
    }

    /**
     * Get classpath resource as file
     * @param name the name of the resource
     * @return the resource file, or {@link Optional#EMPTY}
     */
    public static Optional<InputStream> resourceAsStream(String name) {
        ClassLoader classLoader = classLoader();
        return Optional.ofNullable(name)
                .map(classLoader::getResourceAsStream);
    }

    public static ClassLoader classLoader() {
        return FileUtils.class.getClassLoader();
    }
}
