package org.activiti.cloud.common.error.attributes;

import java.util.Map;

@FunctionalInterface
public interface ErrorAttributesCustomizer {
    public Map<String, Object> customize(Map<String, Object> errorAttributes, Throwable error);
}
