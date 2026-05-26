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

import java.util.Arrays;
import java.util.Objects;

public record SafeZipEntry(String name, byte[] content, boolean directory) {
    public SafeZipEntry {
        if (content != null) {
            content = Arrays.copyOf(content, content.length);
        }
    }

    public SafeZipEntry(String name, byte[] content) {
        this(name, content, false);
    }

    @Override
    public byte[] content() {
        return content == null ? null : Arrays.copyOf(content, content.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SafeZipEntry that)) {
            return false;
        }
        return directory == that.directory && Objects.equals(name, that.name) && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, Arrays.hashCode(content), directory);
    }
}
