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

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.springframework.web.multipart.MultipartFile;

public final class SafeZipStream {

    private final InputStream inputStream;
    private final SafeZipLimits limits;

    private SafeZipStream(InputStream inputStream, SafeZipLimits limits) {
        this.inputStream = inputStream;
        this.limits = limits;
    }

    public static SafeZipStream of(InputStream inputStream, SafeZipLimits limits) {
        return new SafeZipStream(inputStream, limits);
    }

    public static SafeZipStream of(MultipartFile multipartFile, SafeZipLimits limits) throws IOException {
        return new SafeZipStream(multipartFile.getInputStream(), limits);
    }

    public void forEach(Consumer<SafeZipStreamEntry> consumer) throws IOException {
        try {
            SafeZipExtractor.forEachStreamEntry(
                inputStream,
                limits,
                entry -> consumer.accept(SafeZipStreamEntry.from(entry))
            );
        } catch (SafeZipException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
