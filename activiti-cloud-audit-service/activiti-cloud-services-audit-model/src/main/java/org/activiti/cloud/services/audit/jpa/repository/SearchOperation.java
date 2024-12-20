/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.audit.jpa.repository;

public enum SearchOperation {
    EQUALITY,
    NEGATION,
    GREATER_THAN,
    GREATER_THAN_EQUAL,
    LESS_THAN,
    LESS_THAN_EQUAL,
    LIKE,
    STARTS_WITH,
    ENDS_WITH,
    CONTAINS;

    public static final String[] SIMPLE_OPERATION_SET = { ":", "!", ">", "<", "~" };

    public static final String OR_PREDICATE_FLAG = "'";

    public static final String ZERO_OR_MORE_REGEX = "*";

    public static final String OR_OPERATOR = "OR";

    public static final String AND_OPERATOR = "AND";

    public static final String LEFT_PARANTHESIS = "(";

    public static final String RIGHT_PARANTHESIS = ")";

    public static SearchOperation getSimpleOperation(final String operation) {
        char input = operation.charAt(0);
        switch (input) {
            case ':':
                return EQUALITY;
            case '!':
                return NEGATION;
            case '>':
                return operation.length() > 1 && operation.charAt(1) == '=' ? GREATER_THAN_EQUAL : GREATER_THAN;
            case '<':
                return operation.length() > 1 && operation.charAt(1) == '=' ? LESS_THAN_EQUAL : LESS_THAN;
            case '~':
                return LIKE;
            default:
                return null;
        }
    }
}
