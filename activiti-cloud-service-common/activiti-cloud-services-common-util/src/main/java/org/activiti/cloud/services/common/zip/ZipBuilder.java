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
package org.activiti.cloud.services.common.zip;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_ZIP;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.activiti.cloud.services.common.file.FileContent;

/**
 * Builder for zip content
 */
public class ZipBuilder {

    private static final String ZIP_PATH_DELIMITATOR = "/";

    private String name;

    private Set<String> entries = new TreeSet<>();

    private Map<String, byte[]> contentMap = new HashMap<>();

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public ZipBuilder(String name) {
        this.name = name;
    }

    /**
     * Append a folder to the zip content. The path of the folder to be appended is given as an array of folder names.
     *
     * @param path the path of the folder
     * @return this
     */
    public ZipBuilder appendFolder(String... path) {
        String entry = Arrays.stream(path).collect(Collectors.joining(ZIP_PATH_DELIMITATOR, "", ZIP_PATH_DELIMITATOR));
        entries.add(entry);
        return this;
    }

    /**
     * Append a file to the zip content. The path of the file to be appended is given as an array of folder names ended
     * with the file name.
     *
     * @param content the file content
     * @param path    the path of the file
     * @return this
     */
    public ZipBuilder appendFile(byte[] content, String... path) {
        String entry = String.join(ZIP_PATH_DELIMITATOR, path);
        entries.add(entry);
        contentMap.put(entry, content);
        return this;
    }

    /**
     * Append a file to the zip content. The path of the file to be appended is given as an array of folder names. The
     * file name will be appended to this path.
     *
     * @param fileContent the file content
     * @param path        the folders path
     * @return this
     */
    public ZipBuilder appendFile(FileContent fileContent, String... path) {
        String[] newPath = Arrays.copyOf(path, path.length + 1);
        newPath[path.length] = fileContent.getFilename();
        return appendFile(fileContent.getFileContent(), newPath);
    }

    /**
     * Build the zip content stream based on collected folder and files.
     *
     * @return the zip content as byte array output stream
     * @throws IOException in case of I/O error
     */
    public ByteArrayOutputStream toByteArrayOutputStream() throws IOException {
        try (
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)
        ) {
            for (String entry : entries) {
                zipOutputStream.putNextEntry(new ZipEntry(entry));
                byte[] content = contentMap.get(entry);
                if (content != null) {
                    writeChunked(content, zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
            zipOutputStream.close();
            return outputStream;
        }
    }

    /**
     * Write the zip content into the provided file.
     *
     * @param file the file where the zip file will be written to
     * @throws IOException in case of I/O error
     */
    public void writeToFile(File file) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            toByteArrayOutputStream().writeTo(outputStream);
        }
    }

    /**
     * Build the zip content based on collected folder and files.
     *
     * @return the zip content as byte array
     * @throws IOException in case of I/O error
     */
    public byte[] toZipBytes() throws IOException {
        return toByteArrayOutputStream().toByteArray();
    }

    /**
     * Build the zip content as {@link FileContent}
     *
     * @return the {@link FileContent}
     * @throws IOException in case of I/O error
     */
    public FileContent toZipFileContent() throws IOException {
        return new FileContent(name + ".zip", CONTENT_TYPE_ZIP, toZipBytes());
    }

    private void writeChunked(byte[] data, ZipOutputStream output) throws IOException {
        if (data != null) {
            int bytes = data.length;
            int offset = 0;
            while (bytes > 0) {
                final int chunk = Math.min(bytes, DEFAULT_BUFFER_SIZE);
                output.write(data, offset, chunk);
                bytes -= chunk;
                offset += chunk;
            }
        }
    }
}
