/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.acc.core.assertions;

import javax.servlet.http.HttpServletResponse;

import feign.FeignException;
import net.thucydides.core.steps.StepEventBus;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.*;

/**
 * Rest errors assertions
 */
public class RestErrorAssert {

    private FeignException exception;

    private RestErrorAssert(FeignException exception) {
        this.exception = exception;
    }

    public static RestErrorAssert assertThatFeignExceptionIsThrownBy(final ThrowingCallable throwingCallable) {
        Throwable throwable = catchThrowable(throwingCallable);
        assertThat(throwable).isInstanceOf(FeignException.class);

        StepEventBus
                .getEventBus()
                .getBaseStepListener()
                .exceptionExpected(FeignException.class);

        return new RestErrorAssert((FeignException) throwable);
    }

    public static RestErrorAssert assertThatRestNotFoundErrorIsThrownBy(final ThrowingCallable throwingCallable) {
        return assertThatFeignExceptionIsThrownBy(throwingCallable).withNotFoundCode();
    }

    public static RestErrorAssert assertThatRestBadRequestErrorIsThrownBy(final ThrowingCallable throwingCallable) {
        return assertThatFeignExceptionIsThrownBy(throwingCallable).withBadRequestCode();
    }

    public static RestErrorAssert assertThatRestInternalServerErrorIsThrownBy(final ThrowingCallable throwingCallable) {
        return assertThatFeignExceptionIsThrownBy(throwingCallable).withInternalServerErrorCode();
    }

    public RestErrorAssert withErrorCode(int expectedCode) {
        assertThat(exception.status()).isEqualTo(expectedCode);
        return this;
    }

    public RestErrorAssert withMessage(String expectedMessage) {
        assertThat(exception.contentUTF8()).isEqualTo(expectedMessage);
        return this;
    }

    public RestErrorAssert withMessageContaining(String expectedMessage) {
        assertThat(exception.contentUTF8()).contains(expectedMessage);
        return this;
    }

    public RestErrorAssert withBadRequestCode() {
        return withErrorCode(HttpServletResponse.SC_BAD_REQUEST);
    }

    public RestErrorAssert withNotFoundCode() {
        return withErrorCode(HttpServletResponse.SC_NOT_FOUND);
    }

    public RestErrorAssert withInternalServerErrorCode() {
        return withErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

}
