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
package org.activiti.cloud.services.rest.conf;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * An {@link HttpServletRequestWrapper} that replaces the input stream with a
 * {@link ByteCountingInputStream}. When the cumulative bytes read exceed the
 * configured limit, a {@link RequestBodyTooLargeException} is thrown.
 */
public class SizeLimitedRequestWrapper extends HttpServletRequestWrapper {

    private final long maxBytes;
    private ServletInputStream wrappedStream;

    public SizeLimitedRequestWrapper(HttpServletRequest request, long maxBytes) {
        super(request);
        this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (wrappedStream == null) {
            wrappedStream = new ByteCountingInputStream(super.getInputStream(), maxBytes);
        }
        return wrappedStream;
    }
}
