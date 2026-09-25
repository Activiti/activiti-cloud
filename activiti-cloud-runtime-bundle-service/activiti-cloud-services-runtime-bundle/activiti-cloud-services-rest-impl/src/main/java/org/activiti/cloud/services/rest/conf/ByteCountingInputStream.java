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

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.IOException;

/**
 * A {@link ServletInputStream} decorator that counts every byte read and throws
 * {@link RequestBodyTooLargeException} if the total exceeds the allowed maximum.
 */
public class ByteCountingInputStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private final long maxBytes;
    private long bytesRead = 0;

    public ByteCountingInputStream(ServletInputStream delegate, long maxBytes) {
        this.delegate = delegate;
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        int b = delegate.read();
        if (b != -1) {
            bytesRead++;
            checkLimit();
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int count = delegate.read(b, off, len);
        if (count > 0) {
            bytesRead += count;
            checkLimit();
        }
        return count;
    }

    private void checkLimit() {
        if (bytesRead > maxBytes) {
            throw new RequestBodyTooLargeException(bytesRead, maxBytes);
        }
    }

    @Override
    public boolean isFinished() {
        return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
        return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        delegate.setReadListener(readListener);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
