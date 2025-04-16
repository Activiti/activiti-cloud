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

package org.activiti.cloud.services.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.introproventures.graphql.jpa.query.schema.impl.EntityIntrospector;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
public class ActivitiRestrictedKeysSupplierProviderTest {

    @Autowired
    private ActivitiRestrictedKeysProvider activitiRestrictedKeysProvider;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private UserGroupManager userGroupManager;

    @MockitoBean
    private SecurityManager securityManager;

    @Test
    void contextLoads() {
        // given
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");

        var entity = entityManager.getMetamodel().entity(ProcessDefinitionEntity.class);

        var entityDescriptor = EntityIntrospector.introspect(entity);

        // when
        var result = activitiRestrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isNotEmpty().get().isEqualTo(List.of());
    }
}
