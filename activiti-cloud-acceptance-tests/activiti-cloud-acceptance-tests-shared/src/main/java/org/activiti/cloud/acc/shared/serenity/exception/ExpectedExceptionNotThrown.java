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
package org.activiti.cloud.acc.shared.serenity.exception;

/**
 * RuntimeException thrown when an expected exception was not thrown
 */
public class ExpectedExceptionNotThrown extends RuntimeException {

    private ExpectedException expectedExeption;

    public ExpectedExceptionNotThrown(ExpectedException expectedExeption) {
        this.expectedExeption = expectedExeption;
    }

    public ExpectedExceptionNotThrown(String message, ExpectedException expectedExeption) {
        super(message);
        this.expectedExeption = expectedExeption;
    }

    public ExpectedException getExpectedExeption() {
        return expectedExeption;
    }
}
