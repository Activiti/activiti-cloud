package org.activiti.cloud.services.audit.api;

public class AuditException extends RuntimeException {

    public AuditException(String message) {
        super(message);
    }

    public AuditException(String message,
                          Throwable cause) {
        super(message,
              cause);
    }
}
