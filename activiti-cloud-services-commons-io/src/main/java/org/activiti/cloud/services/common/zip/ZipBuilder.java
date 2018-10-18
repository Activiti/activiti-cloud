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

package org.activiti.cloud.services.common.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.activiti.cloud.services.common.file.FileContent;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_ZIP;
import static org.apache.commons.io.IOUtils.writeChunked;

/**
 * Builder for zip content
 */
public class ZipBuilder {

    private static final String ZIP_PATH_DELIMITATOR = "/";

    private String name;

    private Set<String> entries = new TreeSet<>();

    private Map<String, byte[]> contentMap = new HashMap<>();

    public ZipBuilder(String name) {
        this.name = name;
    }

    /**
     * Append a folder to the zip content.
     * The path of the folder to be appended is given as an array of folder names.
     * @param path the path of the folder
     * @return this
     */
    public ZipBuilder appendFolder(String... path) {
        String entry = Arrays.stream(path)
                .collect(Collectors.joining(ZIP_PATH_DELIMITATOR,
                                            "",
                                            ZIP_PATH_DELIMITATOR));
        entries.add(entry);
        return this;
    }

    /**
     * Append a file to the zip content.
     * The path of the foile to be appended is given as an array of folder names ended with the file name.
     * @param content the file content
     * @param path the path of the file
     * @return this
     */
    public ZipBuilder appendFile(byte[] content,
                                 String... path) {
        String entry = String.join(ZIP_PATH_DELIMITATOR,
                                   path);
        entries.add(entry);
        contentMap.put(entry,
                       content);
        return this;
    }

    /**
     * Build the zip content based on collected folder and files.
     * @return the zip content as byte array
     * @throws IOException in case of I/O error
     */
    public byte[] toZipBytes() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (String entry : entries) {
                zipOutputStream.putNextEntry(new ZipEntry(entry));
                byte[] content = contentMap.get(entry);
                if (content != null) {
                    writeChunked(content,
                                 zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
            zipOutputStream.close();
            return outputStream.toByteArray();
        }
    }

    /**
     * Build the zip content as {@link FileContent}
     * @return the {@link FileContent}
     * @throws IOException in case of I/O error
     */
    public FileContent toZipFileContent() throws IOException {
        return new FileContent(name + ".zip",
                               CONTENT_TYPE_ZIP,
                               toZipBytes());
    }
}
