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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.web.multipart.MultipartFile;

import static org.apache.commons.io.IOUtils.toByteArray;

/**
 * Zip stream based on a {@link ZipInputStream}
 */
public class ZipStream {

    private final InputStream inputStream;

    public ZipStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Performs an action for each zip entry of this stream.
     * @param consumer the action to perform on the zip entries
     * @throws IOException in case of zip entry input stream access error
     */
    public void forEach(Consumer<ZipStreamEntry> consumer) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                consumer.accept(new ZipStreamEntry(zipEntry,
                                                   zipInputStream));
            }
        }
    }

    /**
     * Create thg {@link ZipStream} corresponding to a {@link MultipartFile}.
     * @param multipartFile the multipart file
     * @return the zip stream
     * @throws IOException in case of multipart file input stream access error
     */
    public static ZipStream of(MultipartFile multipartFile) throws IOException {
        return new ZipStream(multipartFile.getInputStream());
    }

    /**
     * Create thg {@link ZipStream} corresponding to an {@link InputStream}.
     * @param inputStream the inputStream
     * @return the zip stream
     * @throws IOException in case of input stream access error
     */
    public static ZipStream of(InputStream inputStream) throws IOException {
        return new ZipStream(inputStream);
    }

    /**
     * Wrapper over {@link ZipEntry} used in {@link ZipStream}
     */
    public class ZipStreamEntry {

        private final ZipEntry zipEntry;

        private final Optional<byte[]> content;

        private final Path path;

        ZipStreamEntry(ZipEntry zipEntry,
                       ZipInputStream zipInputStream) throws IOException {
            this.zipEntry = zipEntry;
            this.content = !zipEntry.isDirectory() ?
                    Optional.of(toByteArray(zipInputStream)) :
                    Optional.empty();
            path = Paths.get(zipEntry.getName());
        }

        /**
         * Get the full name of the zip entry
         * @return zip entry name
         */
        public String getName() {
            return zipEntry.getName();
        }

        /**
         * Get the folder name corresponding to an index.
         * <p>
         * <p> The {@code index} parameter is the index of the folder name to return.
         * The the root folder in the directory hierarchy has index {@code 0}.
         * <p>
         * If the zip entry is a file, will return {@literal Optional#empty()}
         * for the index value corresponding to the file name.
         * @param index the index of the folder
         * @return the folder name corresponding to the given index,
         * or {@literal Optional#empty()} if there is no folder for that index
         */
        public Optional<String> getFolderName(int index) {
            Path folderPath = null;
            int foldersCount = zipEntry.isDirectory() ?
                    path.getNameCount() :
                    path.getNameCount() - 1;
            if (0 <= index && index < foldersCount) {
                folderPath = path.getName(index);
            }

            return Optional.ofNullable(folderPath)
                    .map(Path::toString);
        }

        /**
         * Get the file name or the folder name corresponding to this zip entry.
         * @return the file name
         */
        public String getFileName() {
            return path.getFileName().toString();
        }

        /**
         * Get the file content for this zip entry.
         * If this is not a file, this will retunrn {@literal Optional#empty()}
         * @return the file content, or {@literal Optional#empty()}
         */
        public Optional<byte[]> getContent() {
            return content;
        }
    }
}
