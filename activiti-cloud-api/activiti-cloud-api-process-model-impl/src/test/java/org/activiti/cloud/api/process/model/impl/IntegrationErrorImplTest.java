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
package org.activiti.cloud.api.process.model.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.stream.Stream;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationErrorImplTest {

    @Mock
    private IntegrationRequest integrationRequest;

    @Mock
    private IntegrationContext integrationContext;

    @BeforeEach
    void setUp() {
        given(integrationRequest.getIntegrationContext()).willReturn(integrationContext);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    void should_returnRootCauseMessage_when_errorMessageIsNullOrBlank(String errorMessage) {
        parametrizedAssertion(errorMessage, "Root cause message", "Root cause message");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    void should_returnErrorMessage_when_rootCauseMessageIsNullOrBlank(String rootCauseMessage) {
        parametrizedAssertion("Error message", rootCauseMessage, "Error message");
    }

    @ParameterizedTest
    @MethodSource("equalMessagesProvider")
    void should_returnRootCauseMessage_when_messagesAreEqualIgnoringCase(
        String errorMessage,
        String rootCauseMessage,
        String expected
    ) {
        parametrizedAssertion(errorMessage, rootCauseMessage, expected);
    }

    static Stream<Arguments> equalMessagesProvider() {
        return Stream.of(
            Arguments.of("Same message", "Same message", "Same message"),
            Arguments.of("SAME MESSAGE", "same message", "same message")
        );
    }

    @ParameterizedTest
    @MethodSource("combinedMessagesProvider")
    void should_returnCombinedMessage_when_messagesAreDifferent(Throwable error, String expected) {
        // when
        var result = new IntegrationErrorImpl(integrationRequest, error);

        // then
        assertThat(result.getErrorMessage()).isEqualTo(expected);
    }

    static Stream<Arguments> combinedMessagesProvider() {
        var simpleError = new RuntimeException("Error message", new RuntimeException("Root cause message"));
        var deepRootCause = new RuntimeException("Deep root cause");
        var intermediateCause = new RuntimeException("Intermediate cause", deepRootCause);
        var nestedError = new RuntimeException("Top level error", intermediateCause);

        return Stream.of(
            Arguments.of(simpleError, "Error message caused by: Root cause message"),
            Arguments.of(nestedError, "Top level error caused by: Deep root cause")
        );
    }

    @Test
    void should_returnErrorMessage_when_errorHasNoCause() {
        // given
        var error = new RuntimeException("Error message");

        // when
        var result = new IntegrationErrorImpl(integrationRequest, error);

        // then
        assertThat(result.getErrorMessage()).isEqualTo("Error message");
    }

    @ParameterizedTest
    @MethodSource("nullMessagesProvider")
    void should_returnNull_when_bothMessagesAreNull(Throwable error) {
        // when
        var result = new IntegrationErrorImpl(integrationRequest, error);

        // then
        assertThat(result.getErrorMessage()).isNull();
    }

    static Stream<Arguments> nullMessagesProvider() {
        return Stream.of(
            Arguments.of(new RuntimeException(null, new RuntimeException((String) null))),
            Arguments.of(new RuntimeException((String) null))
        );
    }

    @ParameterizedTest
    @MethodSource("redundantMessagesProvider")
    void should_returnTheMostInformative_when_messagesAreRedundant(
        String errorMessage,
        String rootCauseMessage,
        String expected
    ) {
        parametrizedAssertion(errorMessage, rootCauseMessage, expected);
    }

    static Stream<Arguments> redundantMessagesProvider() {
        return Stream.of(
            Arguments.of("java.lang.RuntimeException: Error", "Error", "Error"),
            Arguments.of("ERROR", "java.lang.RuntimeException: error", "error")
        );
    }

    @ParameterizedTest
    @MethodSource("jsonMessagesProvider")
    void should_returnJson_when_messagesAreJson(String errorMessage, String rootCauseMessage, String expected) {
        parametrizedAssertion(errorMessage, rootCauseMessage, expected);
    }

    private void parametrizedAssertion(String errorMessage, String rootCauseMessage, String expected) {
        // given
        var rootCause = new RuntimeException(rootCauseMessage);
        var error = new RuntimeException(errorMessage, rootCause);

        // when
        var result = new IntegrationErrorImpl(integrationRequest, error);

        // then
        assertThat(result.getErrorMessage()).isEqualTo(expected);
    }

    static Stream<Arguments> jsonMessagesProvider() {
        var jsonError = "{\"message\":\"Dmn table notDefined-v2.dmn not valid or not found\",\"severity\":\"ERROR\"}";
        return Stream.of(Arguments.of(jsonError, "Error", jsonError), Arguments.of("ERROR", jsonError, jsonError));
    }

    @Test
    void should_preserveOriginalMessage_when_messageHasNoColon() {
        // given
        var rootCause = new RuntimeException("Simple error without colon");
        var error = new RuntimeException("Another simple message", rootCause);

        // when
        var result = new IntegrationErrorImpl(integrationRequest, error);

        // then
        assertThat(result.getErrorMessage()).isEqualTo("Another simple message caused by: Simple error without colon");
    }

    @Test
    void should_preserveOriginalMessage_when_prefixIsNotValidClassName() {
        // given
        var rootCause = new RuntimeException("com.nonexistent.FakeClass: root error");
        var error = new RuntimeException("org.fake.NotARealClass: top error", rootCause);

        // when
        var result = new IntegrationErrorImpl(integrationRequest, error);

        // then
        assertThat(result.getErrorMessage())
            .isEqualTo("org.fake.NotARealClass: top error caused by: com.nonexistent.FakeClass: root error");
    }

    @Test
    void should_removeClassName_when_prefixIsValidClassName() {
        // given
        var rootCause = new RuntimeException("java.lang.IllegalArgumentException: invalid argument");
        var error = new RuntimeException("java.lang.RuntimeException: runtime failure", rootCause);

        // when
        var result = new IntegrationErrorImpl(integrationRequest, error);

        // then
        assertThat(result.getErrorMessage()).isEqualTo("runtime failure caused by: invalid argument");
    }

    @ParameterizedTest
    @MethodSource("classNamePrefixedMessagesProvider")
    void should_handleClassNamePrefix_when_messageStartsWithClassName(
        String errorMessage,
        String rootCauseMessage,
        String expected
    ) {
        parametrizedAssertion(errorMessage, rootCauseMessage, expected);
    }

    static Stream<Arguments> classNamePrefixedMessagesProvider() {
        return Stream.of(
            // Valid class name is stripped
            Arguments.of(
                "java.lang.NullPointerException: value is null",
                "simple root",
                "value is null caused by: simple root"
            ),
            // Invalid class name prefix is preserved
            Arguments.of("NotAClass: some error", "root message", "NotAClass: some error caused by: root message"),
            // Colon in middle of message without class prefix
            Arguments.of("Error: details: more info", "root", "Error: details: more info caused by: root")
        );
    }

    @Test
    void should_returnRootCauseMessage_when_errorMessageContainsOnlyClassName() {
        // given
        var rootCause = new RuntimeException("actual error message");
        var error = new RuntimeException("java.lang.RuntimeException: actual error message", rootCause);

        // when
        var result = new IntegrationErrorImpl(integrationRequest, error);

        // then
        assertThat(result.getErrorMessage()).isEqualTo("actual error message");
    }
}
