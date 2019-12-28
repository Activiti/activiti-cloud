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

package org.activiti.cloud.services.modeling.jpa.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.modeling.jpa.config.ModelingJpaApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ModelingJpaApplication.class)
public class AuditorAwareIT {

    @Autowired
    private AuditorAware<String> auditorAware;

    @MockBean
    private SecurityManager securityManager;

    @Test
    public void testCurrentAuditor() {

        // GIVEN
        when(securityManager.getAuthenticatedUserId()).thenReturn("test_user");

        // WHEN
        assertThat(auditorAware.getCurrentAuditor()).hasValueSatisfying(
                currentUser ->
                        // THEN
                        assertThat(currentUser).isEqualTo("test_user")
        );
    }

}
