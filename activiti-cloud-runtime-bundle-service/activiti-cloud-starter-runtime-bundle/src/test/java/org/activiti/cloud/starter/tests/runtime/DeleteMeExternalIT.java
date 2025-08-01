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
package org.activiti.cloud.starter.tests.runtime;

import org.activiti.cloud.api.DeleteMe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * External integration test for DeleteMe class from runtime bundle module
 */
public class DeleteMeExternalIT {
    
    @Test
    public void testCoveredByExternalIT() {
        DeleteMe deleteMe = new DeleteMe();
        assertEquals("covered by an integration test from another module", deleteMe.coveredByExternalIT());
    }
}
