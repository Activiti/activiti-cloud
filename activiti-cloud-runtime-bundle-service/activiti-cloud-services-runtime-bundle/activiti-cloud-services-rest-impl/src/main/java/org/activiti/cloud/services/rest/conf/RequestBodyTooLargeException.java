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

/**
 * Exception thrown when the actual bytes read from the request body exceed the allowed limit.
 */
public class RequestBodyTooLargeException extends RuntimeException {

    private final long bytesRead;

    public RequestBodyTooLargeException(long bytesRead, long maxAllowed) {
        super("Request body of " + bytesRead + " bytes exceeds the maximum allowed size of " + maxAllowed + " bytes");
        this.bytesRead = bytesRead;
    }

    public long getBytesRead() {
        return bytesRead;
    }
}
