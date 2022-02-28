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
package org.activiti.cloud.services.modeling.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import org.activiti.cloud.modeling.api.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelTypeComparatorTest {

    Comparator<Model> modelTypeComparator = new ModelTypeComparator();

    Model formModel;
    Model testModel;

    @BeforeEach
    public void setUp() {
        formModel = mock(Model.class);
        testModel = mock(Model.class);

        when(formModel.getType()).thenReturn("FORM");
        when(testModel.getType()).thenReturn("TEST");
    }

    @Test
    public void testCompare() throws Exception {
        assertEquals(modelTypeComparator.compare(formModel, testModel), -1);
        assertEquals(modelTypeComparator.compare(formModel, formModel), 0);
        assertEquals(modelTypeComparator.compare(testModel, formModel), 1);
    }
}
