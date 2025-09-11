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

import { MultipleRuntimeBundleService } from '../services/multiple-runtime-bundle.service';
import { SecurityPoliciesService } from '../services/security-policies.service';
import { IdentityManagementService } from '../services/identity-management.service';
import { contexts } from './context.fixture';

interface ServicesFixture {
  multipleRuntimeServiceTestUser: MultipleRuntimeBundleService;
  securityPoliciesServiceHrUser: SecurityPoliciesService;
  securityPoliciesServiceProcessAdmin: SecurityPoliciesService;
  identityManagementServiceTestUser: IdentityManagementService;
}

const activiti = contexts.extend<ServicesFixture>({
  multipleRuntimeServiceTestUser: async ({ testUserContext }, use) => {
    await use(new MultipleRuntimeBundleService(testUserContext));
  },

  securityPoliciesServiceHrUser: async ({ hrUserContext }, use) => {
    await use(new SecurityPoliciesService(hrUserContext));
  },

  securityPoliciesServiceProcessAdmin: async ({ processAdminContext }, use) => {
    await use(new SecurityPoliciesService(processAdminContext));
  },

  identityManagementServiceTestUser: async ({ testUserContext }, use) => {
    await use(new IdentityManagementService(testUserContext));
  }
});

export { activiti }
