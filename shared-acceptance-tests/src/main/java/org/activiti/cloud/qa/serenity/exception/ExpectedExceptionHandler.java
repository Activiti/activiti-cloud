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

package org.activiti.cloud.qa.serenity.exception;

import net.serenitybdd.core.Serenity;

import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

/**
 * Expected exception handler
 */
public class ExpectedExceptionHandler {

    private static final String EXPECTED_EXCEPTION = "expectedException";

    /**
     * Ask if a call throws an expected exception.
     * @param expectedException expected exception
     * @param throwingCallable the callable
     */
    public static boolean isThrowingExpectedException(ExpectedException expectedException,
                                                      ThrowingCallable throwingCallable) {
        try {
            Serenity.setSessionVariable(EXPECTED_EXCEPTION).to(expectedException);
            return expectedException.isExpectedException(catchThrowable(throwingCallable));
        } finally {
            Serenity.setSessionVariable(EXPECTED_EXCEPTION).to(null);
        }
    }

    /**
     * Ask if an exception is expected.
     * @return true if an exception is expected
     */
    public static boolean isExpectingException() {
        return Serenity.hasASessionVariableCalled(EXPECTED_EXCEPTION);
    }
}
