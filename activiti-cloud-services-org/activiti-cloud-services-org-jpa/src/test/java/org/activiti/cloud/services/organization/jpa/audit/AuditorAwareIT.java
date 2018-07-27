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

package org.activiti.cloud.services.organization.jpa.audit;

import org.activiti.cloud.services.organization.jpa.config.OrganizationJpaApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationJpaApplication.class)
public class AuditorAwareIT {

    @Autowired
    private AuditorAware<String> auditorAware;

    @Test
    public void testCurrentAuditor() {

        // WHEN
        assertThat(auditorAware.getCurrentAuditor()).hasValueSatisfying(
                currentUser ->
                        // THEN
                        assertThat(currentUser).isEqualTo("Unknown")
        );

        // GIVEN
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new User("test_user",
                                                                 "test_password",
                                                                 emptyList()),
                                                        null)
        );

        // WHEN
        assertThat(auditorAware.getCurrentAuditor()).hasValueSatisfying(
                currentUser ->
                        // THEN
                        assertThat(currentUser).isEqualTo("test_user")
        );
    }
}
