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
package org.activiti.cloud.services.rest.assemblers;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ToCandidateGroupConverterTest {

    private ToCandidateGroupConverter toCandidateGroupConverter;

    @BeforeEach
    public void setUp() {
        toCandidateGroupConverter = new ToCandidateGroupConverter();
    }

    @Test
    public void shouldConvertStringGroupsToCanidateGroups() {
        //given
        String group = "group1";
        List<String> groupList = new ArrayList<>();
        groupList.add(group);
        //when
        List<CandidateGroup> convertedGroupList = toCandidateGroupConverter.from(groupList);
        //then
        assertThat(convertedGroupList.get(0)).isInstanceOf(CandidateGroup.class);
        assertThat(convertedGroupList.get(0).getGroup()).isEqualTo(group);
    }
}
