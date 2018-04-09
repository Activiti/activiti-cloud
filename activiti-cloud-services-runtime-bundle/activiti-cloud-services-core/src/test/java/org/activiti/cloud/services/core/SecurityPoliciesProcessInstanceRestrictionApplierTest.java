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

package org.activiti.cloud.services.core;

import java.util.Collections;
import java.util.Set;

import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SecurityPoliciesProcessInstanceRestrictionApplierTest {

    private SecurityPoliciesProcessInstanceRestrictionApplier restrictionApplier = new SecurityPoliciesProcessInstanceRestrictionApplier();

    @Test
    public void restrictToKeysAddFilterOnGivenKeys() {
        //given
        ProcessInstanceQuery initialQuery = mock(ProcessInstanceQuery.class);
        Set<String> keys = Collections.singleton("procDef");

        ProcessInstanceQuery restrictedQuery = mock(ProcessInstanceQuery.class);
        given(initialQuery.processDefinitionKeys(keys)).willReturn(restrictedQuery);

        //when
        ProcessInstanceQuery resultQuery = restrictionApplier.restrictToKeys(initialQuery,
                                                                               keys);

        //then
        assertThat(resultQuery).isEqualTo(restrictedQuery);
    }

    @Test
    public void denyAllShouldAddUnmatchableFilter() {
        //given
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        ProcessInstanceQuery restrictedQuery = mock(ProcessInstanceQuery.class);
        given(query.processDefinitionId(anyString())).willReturn(restrictedQuery);

        //when
        ProcessInstanceQuery resultQuery = restrictionApplier.denyAll(query);

        //then
        assertThat(resultQuery).isEqualTo(restrictedQuery);
        verify(query).processDefinitionId(startsWith("missing-"));
    }

}