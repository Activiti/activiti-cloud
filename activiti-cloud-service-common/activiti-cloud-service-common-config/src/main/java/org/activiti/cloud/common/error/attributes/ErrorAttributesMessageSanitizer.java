package org.activiti.cloud.common.error.attributes;

import java.util.Arrays;
import java.util.Map;


public class ErrorAttributesMessageSanitizer implements ErrorAttributesCustomizer {

    private static final String MESSAGE = "message";
    private static final String ERROR_NOT_DISCLOSED_MESSAGE = "An exception occurred. The full error message is not disclosed for security reasons.";
    private static final String[] TECHNICAL_INFO_BLACKLIST = {
        "java.",
        "javax.",
        "org.",
        "com.",
        "net.",
        "io.",
    };

    @Override
    public Map<String, Object> customize(Map<String, Object> errorAttributes, Throwable error) {
        if (errorAttributes.containsKey(MESSAGE)) {
            final String message = (String) errorAttributes.get(MESSAGE);
            final String censoredMessage = containsTechnicalInfo(message) ? ERROR_NOT_DISCLOSED_MESSAGE : message;
            errorAttributes.put(MESSAGE, censoredMessage);
        }

        return errorAttributes;
    }

    private boolean containsTechnicalInfo(String message) {
        return Arrays.stream(TECHNICAL_INFO_BLACKLIST).anyMatch(message::contains);
    }
}
