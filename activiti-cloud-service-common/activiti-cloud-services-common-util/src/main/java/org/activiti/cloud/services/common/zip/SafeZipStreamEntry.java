/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import java.nio.file.Path;
import java.util.Optional;

public final class SafeZipStreamEntry {

    private final String name;
    private final byte[] content;
    private final boolean directory;

    SafeZipStreamEntry(String name, byte[] content, boolean directory) {
        this.name = name;
        this.content = content;
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return ZipEntryPaths.getFileName(name, directory);
    }

    public Optional<String> getFolderName(int level) {
        return ZipEntryPaths.getFolderName(name, directory, level);
    }

    public Optional<byte[]> getContent() {
        if (directory) {
            return Optional.empty();
        }
        return Optional.ofNullable(content);
    }

    public boolean isDirectory() {
        return directory;
    }

    public boolean isAtRoot() {
        return Path.of(ZipEntryPaths.normalizeEntryName(name)).getNameCount() == 1;
    }
}
