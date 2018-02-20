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

package org.activiti.cloud.qa.modeling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.modeling.model.Group;

/**
 * Modeling steps
 */
public class ModelingSteps {

    @Step
    public void createGroup(String groupId,
                            String groupName) {
        ModelingClient
                .get()
                .create(new Group(groupId,
                                  groupName));
    }

    @Step
    public Group findGroupById(String groupId) {
        return ModelingClient
                .get()
                .findById(groupId);
    }

    @Step
    public void checkGroupIsExpectedOne(Group groupToCheck, String expectedGroupName) {
        assertNotNull(groupToCheck);
        assertEquals(expectedGroupName, groupToCheck.getName());
    }
}
