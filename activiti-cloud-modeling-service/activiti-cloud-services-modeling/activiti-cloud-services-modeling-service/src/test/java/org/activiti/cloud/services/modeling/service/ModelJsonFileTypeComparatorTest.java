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
import org.activiti.cloud.modeling.api.ModelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelJsonFileTypeComparatorTest {

    Comparator<ProjectHolder.ModelJsonFile> modelJsonFileTypeComparator = new ModelJsonFileTypeComparator();

    ModelType formModelType;
    ModelType testModelType;
    ProjectHolder.ModelJsonFile formModelJsonFile;
    ProjectHolder.ModelJsonFile testModelJsonFile;

    @BeforeEach
    public void setUp() {
        formModelType = mock(ModelType.class);
        testModelType = mock(ModelType.class);
        formModelJsonFile = mock(ProjectHolder.ModelJsonFile.class);
        testModelJsonFile = mock(ProjectHolder.ModelJsonFile.class);

        when(formModelJsonFile.getModelType()).thenReturn(formModelType);
        when(testModelJsonFile.getModelType()).thenReturn(testModelType);
        when(formModelType.getName()).thenReturn("FORM");
        when(testModelType.getName()).thenReturn("TEST");
    }

    @Test
    public void testCompare() throws Exception {
        assertEquals(modelJsonFileTypeComparator.compare(formModelJsonFile, testModelJsonFile), -1);
        assertEquals(modelJsonFileTypeComparator.compare(formModelJsonFile, formModelJsonFile), 0);
        assertEquals(modelJsonFileTypeComparator.compare(testModelJsonFile, formModelJsonFile), 1);
    }
}
