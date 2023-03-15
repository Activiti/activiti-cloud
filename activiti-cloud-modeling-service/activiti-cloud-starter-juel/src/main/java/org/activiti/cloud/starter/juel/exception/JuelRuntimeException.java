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
package org.activiti.cloud.starter.juel.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Generic Juel Runtime Exception
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class JuelRuntimeException extends RuntimeException {

    public JuelRuntimeException() {
        super();
    }

    public JuelRuntimeException(Throwable cause) {
        super(cause);
    }

    public JuelRuntimeException(String message) {
        super(message);
    }

    public JuelRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
