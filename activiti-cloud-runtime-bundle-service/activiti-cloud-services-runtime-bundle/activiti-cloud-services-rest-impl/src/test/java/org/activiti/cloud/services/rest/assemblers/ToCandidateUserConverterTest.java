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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ToCandidateUserConverterTest {

    private ToCandidateUserConverter toCandidateUserConverter;

    @BeforeEach
    public void setUp() {
        toCandidateUserConverter = new ToCandidateUserConverter();
    }

    @Test
    public void shouldConvertStringUsersToCanidateUsers() {
        //given
        String user = "user1";
        List<String> userList = new ArrayList<>();
        userList.add(user);
        //when
        List<CandidateUser> convertedUserList = toCandidateUserConverter.from(userList);
        //then
        assertThat(convertedUserList.get(0)).isInstanceOf(CandidateUser.class);
        assertThat(convertedUserList.get(0).getUser()).isEqualTo(user);
    }
}
