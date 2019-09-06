package org.activiti.cloud.organization.validation;

public class ValidationUtil {

    public static final int NAME_MAX_LENGTH = 26;
    public static final String DNS_LABEL_REGEX = "^[a-z]([-a-z0-9]*[a-z0-9])?$";

    public static final String PROJECT_INVALID_EMPTY_NAME = "The project name cannot be empty";
    public static final String PROJECT_INVALID_NAME_LENGTH_MESSAGE =
            "The project name length cannot be greater than " + NAME_MAX_LENGTH;
    public static final String MODEL_INVALID_NAME_LENGTH_MESSAGE =
            "The model name length cannot be greater than " + NAME_MAX_LENGTH;
    public static final String PROJECT_INVALID_NAME_MESSAGE =
            "The project name should follow DNS-1035 conventions: " +
                    "it must consist of lower case alphanumeric characters or '-', " +
                    "and must start and end with an alphanumeric character";
    public static final String MODEL_INVALID_NAME_NULL_MESSAGE =
            "The model name is required";
    public static final String MODEL_INVALID_NAME_EMPTY_MESSAGE =
            "The model name cannot be empty";
    public static final String MODEL_INVALID_NAME_MESSAGE =
            "The model name should follow DNS-1035 conventions: " +
                    "it must consist of lower case alphanumeric characters or '-', " +
                    "and must start and end with an alphanumeric character";

    private ValidationUtil() {
    }
}
