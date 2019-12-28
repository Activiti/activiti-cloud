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

package org.activiti.cloud.services.modeling.asserts;

import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.core.error.SyntacticModelValidationException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.*;

/**
 * Asserts for rest response
 */
public class AssertResponse {

    private final MvcResult response;

    public AssertResponse(MvcResult response) {
        this.response = response;
    }

    public AssertValidationException isValidationException() {
        assertThat(response).isNotNull();
        assertThat(response.getResolvedException()).isNotNull();
        assertThat(response.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class);
        return new AssertValidationException((MethodArgumentNotValidException) response.getResolvedException());
    }

    public AssertValidationException isSemanticValidationException() {
        assertThat(response).isNotNull();
        assertThat(response.getResolvedException()).isNotNull();
        assertThat(response.getResolvedException()).isInstanceOf(SemanticModelValidationException.class);
        return new AssertValidationException((SemanticModelValidationException) response.getResolvedException());
    }

    public AssertValidationException isSyntacticValidationException() {
        assertThat(response).isNotNull();
        assertThat(response.getResolvedException()).isNotNull();
        assertThat(response.getResolvedException()).isInstanceOf(SyntacticModelValidationException.class);
        return new AssertValidationException((SyntacticModelValidationException) response.getResolvedException());
    }

    public static AssertResponse assertThatResponse(ValidatableMockMvcResponse response) {
        return assertThatResponse(response.extract().response().getMvcResult());
    }

    public static AssertResponse assertThatResponse(MvcResult response) {
        return new AssertResponse(response);
    }
}
