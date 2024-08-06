package org.activiti.cloud.services.query.rest;

import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public class IllegalFilterException extends IllegalArgumentException {

    public IllegalFilterException(VariableFilter filter) {
        super("Unsupported type: " + filter.type() + " for operator: " + filter.operator());
    }
}
