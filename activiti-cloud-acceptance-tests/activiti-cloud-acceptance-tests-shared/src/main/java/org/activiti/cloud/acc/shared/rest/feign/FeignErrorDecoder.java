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
package org.activiti.cloud.acc.shared.rest.feign;

import static feign.FeignException.errorStatus;
import static org.activiti.cloud.acc.shared.serenity.exception.ExpectedExceptionHandler.isExpectingException;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.Reader;
import org.activiti.cloud.acc.shared.rest.error.ExpectedRestException;
import org.apache.commons.lang3.StringUtils;

/**
 * Feign error decoder with support for expected errors
 */
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (isExpectingException()) {
            return new ExpectedRestException(response.status(), responseBody(response));
        }

        return errorStatus(methodKey, response);
    }

    private String responseBody(Response response) {
        if (response.body() != null) {
            try (Reader reader = response.body().asReader()) {
                return Util.toString(reader);
            } catch (IOException ignored) {
                //ignore
            }
        }

        return StringUtils.EMPTY;
    }
}
