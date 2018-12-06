/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.query.events.handlers;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskVariableUpdater {

    private final EntityFinder entityFinder;

    private TaskVariableRepository variableRepository;

    @Autowired
    public TaskVariableUpdater(EntityFinder entityFinder,
                           TaskVariableRepository variableRepository) {
        this.entityFinder = entityFinder;
        this.variableRepository = variableRepository;
    }

    public void update(TaskVariableEntity updatedVariableEntity, Predicate predicate, String notFoundMessage) {
        TaskVariableEntity variableEntity = entityFinder.findOne(variableRepository,
                                                             predicate,
                                                             notFoundMessage);
        variableEntity.setLastUpdatedTime(updatedVariableEntity.getLastUpdatedTime());
        variableEntity.setType(updatedVariableEntity.getType());
        variableEntity.setValue(updatedVariableEntity.getValue());

        variableRepository.save(variableEntity);
    }

}
