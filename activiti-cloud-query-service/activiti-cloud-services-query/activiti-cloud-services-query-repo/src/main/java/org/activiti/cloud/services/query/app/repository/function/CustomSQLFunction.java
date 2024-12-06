package org.activiti.cloud.services.query.app.repository.function;

public enum CustomSQLFunction {
    /**
     * Counts the number of rows in the full window frame.
     * If used with a group by clause, it return more than one result, so the result list should be limited to 1;
     */
    COUNT_OVER_FULL_WINDOW,
}
