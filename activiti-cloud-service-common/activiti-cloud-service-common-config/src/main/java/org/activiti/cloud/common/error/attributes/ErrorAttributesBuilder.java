package org.activiti.cloud.common.error.attributes;

import java.util.Map;

public class ErrorAttributesBuilder {

    public static Map<String, Object> build(Map<String, Object> errorAttributes, Throwable error,
                                            ErrorAttributesCustomizer... customizers ) {

        for (ErrorAttributesCustomizer customizer: customizers) {
            errorAttributes = customizer.customize(errorAttributes, error);
        }

        return errorAttributes;
    }
}
