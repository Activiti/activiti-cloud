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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.api.process.model.payloads.CreateProcessInstancePayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.ResolvableType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.ObjectMapper;

class VariableValueSizeValidationAdviceTest {

    private static final long MAX_VALUE_LENGTH = 100;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VariableValueSizeValidationAdvice createAdvice(Set<String> whitelisted) {
        VariableSizeLimitProperties properties = new VariableSizeLimitProperties();
        properties.setMaxVariableValueLength(MAX_VALUE_LENGTH);
        properties.setWhitelistedVariables(whitelisted);
        return new VariableValueSizeValidationAdvice(properties, MAPPER);
    }

    private void setRequestWithVariableName(String variableName) {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "PUT",
            "/v1/tasks/task-1/variables/" + variableName
        );
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("variableName", variableName));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    // --- supports(): decides whether the advice runs for a request body type ---

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        classes = {
            SetProcessVariablesPayload.class,
            StartProcessPayload.class,
            CreateTaskVariablePayload.class,
            UpdateTaskVariablePayload.class,
            SubclassedSetProcessVariablesPayload.class,
        }
    )
    void should_support_variablePayloadTypes(Class<?> payloadType) {
        assertThat(createAdvice(Set.of()).supports(null, payloadType, null)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        classes = { UpdateProcessPayload.class, CreateProcessInstancePayload.class, String.class, Object.class }
    )
    void should_notSupport_otherPayloadTypes(Class<?> payloadType) {
        assertThat(createAdvice(Set.of()).supports(null, payloadType, null)).isFalse();
    }

    @Test
    void should_notSupport_parameterizedTypes() {
        Type listOfStrings = ResolvableType.forClassWithGenerics(List.class, String.class).getType();

        assertThat(createAdvice(Set.of()).supports(null, listOfStrings, null)).isFalse();
    }

    static class SubclassedSetProcessVariablesPayload extends SetProcessVariablesPayload {}

    // --- UpdateTaskVariablePayload: URI name takes precedence over body name ---

    @Test
    void should_rejectUpdateTaskVariable_when_bodyClaimsWhitelistedButUriTargetsNonWhitelisted() {
        VariableValueSizeValidationAdvice advice = createAdvice(Set.of("batchState"));

        // URI targets "ordinaryVar" (not whitelisted), but body claims "batchState" (whitelisted)
        setRequestWithVariableName("ordinaryVar");

        UpdateTaskVariablePayload payload = new UpdateTaskVariablePayload();
        payload.setName("batchState"); // spoofed name
        payload.setValue("x".repeat((int) MAX_VALUE_LENGTH + 1));

        assertThatThrownBy(() -> advice.afterBodyRead(payload, null, null, null, null)).isInstanceOf(
            VariableValueTooLargeException.class
        );
    }

    @Test
    void should_allowUpdateTaskVariable_when_uriTargetsWhitelistedVariable() {
        VariableValueSizeValidationAdvice advice = createAdvice(Set.of("batchState"));

        // URI targets "batchState" (whitelisted)
        setRequestWithVariableName("batchState");

        UpdateTaskVariablePayload payload = new UpdateTaskVariablePayload();
        payload.setName("ordinaryVar"); // body name doesn't matter, URI wins
        payload.setValue("x".repeat(1000));

        assertThatNoException().isThrownBy(() -> advice.afterBodyRead(payload, null, null, null, null));
    }

    @Test
    void should_rejectUpdateTaskVariable_when_uriTargetsNonWhitelistedAndValueExceedsLimit() {
        VariableValueSizeValidationAdvice advice = createAdvice(Set.of("batchState"));

        setRequestWithVariableName("normalVar");

        UpdateTaskVariablePayload payload = new UpdateTaskVariablePayload();
        payload.setName("normalVar");
        payload.setValue("x".repeat((int) MAX_VALUE_LENGTH + 1));

        assertThatThrownBy(() -> advice.afterBodyRead(payload, null, null, null, null)).isInstanceOf(
            VariableValueTooLargeException.class
        );
    }

    @Test
    void should_allowUpdateTaskVariable_when_valueWithinLimit() {
        VariableValueSizeValidationAdvice advice = createAdvice(Set.of("batchState"));

        setRequestWithVariableName("normalVar");

        UpdateTaskVariablePayload payload = new UpdateTaskVariablePayload();
        payload.setName("normalVar");
        payload.setValue("small value");

        assertThatNoException().isThrownBy(() -> advice.afterBodyRead(payload, null, null, null, null));
    }
}
