package org.activiti.cloud.common.swagger;

import static org.activiti.cloud.common.swagger.exceptions.SwaggerExceptionAlreadyExistingOperationId.EXCEPTION_MESSAGE_PATTERN;

import java.util.LinkedHashSet;
import java.util.Set;
import org.activiti.cloud.common.swagger.exceptions.SwaggerExceptionAlreadyExistingOperationId;
import springfox.documentation.OperationNameGenerator;

public class CustomOperationNameGenerator implements OperationNameGenerator {

    private final Set<String> customizedOperationIds = new LinkedHashSet<>();

    @Override
    public String startingWith(String operationId) {

        if (operationId.matches(".*Using(GET|POST|PUT|DELETE)(_[0-9])?")) {
            // Operation Ids that match the springfox format, hence are not customized, are ignored
            return operationId;

        } else if (customizedOperationIds.contains(operationId)) {
            throw new SwaggerExceptionAlreadyExistingOperationId(String.format(EXCEPTION_MESSAGE_PATTERN, operationId));

        } else {
            customizedOperationIds.add(operationId);
            return operationId;
        }
    }
}
