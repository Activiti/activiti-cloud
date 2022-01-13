package org.activiti.cloud.common.swagger;

import springfox.documentation.OperationNameGenerator;

public class CustomOperationNameGenerator implements OperationNameGenerator {

    private static final String DEFAULT_SPRINGFOX_PATTERN_REGEX = "Using(GET|POST|PUT|DELETE)(_[0-9])?";

    @Override
    public String startingWith(String operationId) {
        return operationId.replaceAll(DEFAULT_SPRINGFOX_PATTERN_REGEX, "");
    }
}
