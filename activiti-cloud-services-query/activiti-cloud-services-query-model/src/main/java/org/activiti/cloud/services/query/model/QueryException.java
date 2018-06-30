package org.activiti.cloud.services.query.model;

public class QueryException extends RuntimeException {

    public QueryException(String message) {
        super(message);
    }

    public QueryException(String message,
                          Throwable cause) {
        super(message,
              cause);
    }
}
