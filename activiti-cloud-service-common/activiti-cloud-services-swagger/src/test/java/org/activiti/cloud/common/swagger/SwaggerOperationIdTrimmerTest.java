package org.activiti.cloud.common.swagger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SwaggerOperationIdTrimmerTest {

    private SwaggerOperationIdTrimmer operationIdTrimmer = new SwaggerOperationIdTrimmer();

    private final String DEFAULT_METHOD_NAME_WITH_HTTP_VERB_AND_NUMBER = "findAllUsingGET_2";
    private final String DEFAULT_METHOD_NAME_WITH_HTTP_VERB = "startUsingPOST";

    @Test
    void should_trimHttpVerbAndNumberInDefaultGeneratedMethods() {
        String trimmedMethodName = operationIdTrimmer.startingWith(DEFAULT_METHOD_NAME_WITH_HTTP_VERB_AND_NUMBER);
        assertThat(trimmedMethodName).isEqualTo("findAll");
    }

    @Test
    void should_trimHttpVerbInDefaultGeneratedMethods() {
        String trimmedMethodName = operationIdTrimmer.startingWith(DEFAULT_METHOD_NAME_WITH_HTTP_VERB);
        assertThat(trimmedMethodName).isEqualTo("start");
    }
}
