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
package org.activiti.cloud.api.process.model;

public class CloudBpmnError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public CloudBpmnError(String errorCode) {
        this(errorCode, errorCode);
    }

    public CloudBpmnError(String errorCode, String message) {
        super(message);
        requireValidErrorCode(errorCode);

        this.errorCode = errorCode;
    }

    public CloudBpmnError(String errorCode, Throwable cause) {
        super(cause);
        requireValidErrorCode(errorCode);

        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    protected void requireValidErrorCode(String errorCode) {
        if (errorCode == null) {
            throw new IllegalArgumentException("Error Code must not be null.");
        }
        if (errorCode.length() < 1) {
            throw new IllegalArgumentException("Error Code must not be empty.");
        }
    }
}
