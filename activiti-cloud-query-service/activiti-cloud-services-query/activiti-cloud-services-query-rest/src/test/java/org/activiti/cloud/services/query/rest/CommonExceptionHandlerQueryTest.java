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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletResponse;
import org.activiti.api.model.shared.model.ActivitiErrorMessage;
import org.activiti.cloud.services.query.app.specification.InvalidSortException;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;

class CommonExceptionHandlerQueryTest {

    private final CommonExceptionHandlerQuery handler = new CommonExceptionHandlerQuery();

    @Test
    void should_returnBadRequestWithMessage_forInvalidSortException() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        InvalidSortException ex = new InvalidSortException(
            "Process definition key is required when sorting by process variable"
        );

        EntityModel<ActivitiErrorMessage> result = handler.handleAppException(ex, response);

        assertThat(result.getContent().getMessage()).isEqualTo(
            "Process definition key is required when sorting by process variable"
        );
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
