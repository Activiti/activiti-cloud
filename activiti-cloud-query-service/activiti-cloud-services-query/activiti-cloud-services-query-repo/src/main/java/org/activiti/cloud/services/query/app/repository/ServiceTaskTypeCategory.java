/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.app.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import java.util.Arrays;
import java.util.Optional;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;

/**
 * The broad kinds of service task a client can filter on, as opposed to the full type label stored for a service
 * task, e.g. {@code Rest Connector: GET}. The label is made of the name of what runs the task, optionally followed by
 * {@code ": "} and the action, so a category is recognised from the first part alone.
 */
public enum ServiceTaskTypeCategory {
    SCRIPT("Script") {
        @Override
        BooleanExpression matches(StringPath serviceTaskType) {
            return hasLabel(serviceTaskType, SCRIPT_LABEL);
        }
    },
    AGENT("Agent") {
        @Override
        BooleanExpression matches(StringPath serviceTaskType) {
            return hasLabel(serviceTaskType, AGENT_LABEL);
        }
    },
    CONNECTOR("Connector") {
        @Override
        BooleanExpression matches(StringPath serviceTaskType) {
            return serviceTaskType
                .isNotNull()
                .and(serviceTaskType.ne(ServiceTaskEntity.DEFAULT_SERVICE_TASK_TYPE))
                .and(
                    SCRIPT.matches(serviceTaskType)
                        .or(AGENT.matches(serviceTaskType))
                        .or(hasLabel(serviceTaskType, DMN_LABEL))
                        .or(hasLabel(serviceTaskType, SET_VARIABLES_LABEL))
                        .not()
                );
        }
    };

    // The labels the modeling service generates from the BPMN when a process is released
    private static final String SCRIPT_LABEL = "Script";
    private static final String AGENT_LABEL = "Agentic Task Connector";
    private static final String DMN_LABEL = "DMN Connector";
    private static final String SET_VARIABLES_LABEL = "Set Variables";
    private static final String ACTION_SEPARATOR = ": ";

    private final String name;

    ServiceTaskTypeCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    abstract BooleanExpression matches(StringPath serviceTaskType);

    /**
     * @param value the value of the service task type filter
     * @return the category the value names, ignoring case, or empty if the value is a full type label
     */
    public static Optional<ServiceTaskTypeCategory> from(String value) {
        return Arrays.stream(values())
            .filter(category -> category.name.equalsIgnoreCase(value))
            .findFirst();
    }

    private static BooleanExpression hasLabel(StringPath serviceTaskType, String label) {
        return serviceTaskType.eq(label).or(serviceTaskType.startsWith(label + ACTION_SEPARATOR));
    }
}
